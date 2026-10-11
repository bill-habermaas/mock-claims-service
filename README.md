# Legacy Claims Record Migration Service

This service is the first piece of a replacement for a retiring mainframe claims system. Each night the mainframe produces an 80-character fixed-width export with a header, detail records, and a trailer. The service imports that file, validates every claim against the record layout and business rules, and stores the valid records in MongoDB. It produces a rejection report giving the line number, claim ID, and specific reason for each bad record. A bad record never stops the import, and re-importing the same file creates no duplicates. Claims are served through a REST API: `GET /claims/{claimId}` and `GET /claims?status={code}`, with proper 404 and 400 responses. The stack is Java 17, Spring Boot 3, and MongoDB, and the service and database start together with Docker Compose. It includes unit and API tests.

> Practice project built from a mock coding exercise. All records in `data/legacy_claims_export.txt` are fabricated.

---

## 1. How to build and run

**Prerequisites:** Docker with Compose. For running tests or debugging outside Docker: JDK 17+ and Maven 3.9+.

```bash
docker compose up --build          # starts MongoDB and the service on http://localhost:8080
curl -X POST localhost:8080/imports/sample      # import the bundled sample file
curl localhost:8080/claims/CLM0000001
curl "localhost:8080/claims?status=DN"
docker compose down                # stop (add -v to delete the database volume)
```

Run the tests (no database needed; they use an in-memory claim store):

```bash
mvn test
```

### API

| Method | Path | Result |
|---|---|---|
| `POST` | `/imports` | Import an uploaded file (multipart field `file`). Returns the import summary. |
| `POST` | `/imports/sample` | Import the file at `CLAIMS_SAMPLE_FILE` (in Docker: `./data/legacy_claims_export.txt`). |
| `GET` | `/claims/{claimId}` | One claim, or **404** if unknown. |
| `GET` | `/claims?status={code}` | Claims with that status, or **400** if the code is not OP, AP, DN, or CL. Omit `status` to list all. |
| `GET` | `/actuator/health` | Health check. |

Errors use the standard `application/problem+json` format with a plain-language `detail`.

The import summary returns `recordsRead`, `recordsAccepted`, `recordsRejected`, a `rejections` list (line number, claim ID, reason), and a `warnings` list for records that loaded but need attention. On the sample file: 39 read, 30 accepted, 9 rejected, 2 warnings.

### Working in VS Code

Open the folder and accept the recommended extensions (Java, Spring Boot, Docker, MongoDB, REST Client).

- **Run and debug:** *Run and Debug → Run service (local MongoDB)*. It starts only the MongoDB container, then runs the app from VS Code so breakpoints work.
- **Tasks** (*Terminal → Run Task*): Run tests, Docker Compose up/down, Start MongoDB only, Import sample file.
- **Try the API:** open `requests.http` and click *Send Request* above any block.
- **Browse data:** connect the MongoDB extension to `mongodb://localhost:27017` and open the `claims` database.

### Code layout

| Package | Responsibility |
|---|---|
| `layout` | The record layout (every field position defined once in `RecordLayout`) and splitting the file into header, details, trailer. |
| `validation` | `ClaimValidator` holds every rule. **To add a rule, add one entry to its `FIELD_RULES` list.** `ClaimMapper` turns a valid line into a claim. |
| `importing` | `ImportService` runs an import and applies the four decisions below. |
| `claim` | The `Claim` document, status codes, and storage (`ClaimStore` interface, MongoDB implementation). |
| `web` | REST controllers. |

---

## 2. Decisions on the four undefined cases

The agency gave no rule for these. Each is a single, clearly marked place in `ImportService` or `ClaimValidator`, and each has a test, so any of them can be changed if the agency decides otherwise.

**1. Date of birth recorded as `00000000` (unknown).**
*Decision:* Load the claim with no date of birth, and list it as a warning in the import report.
*Why:* A missing birth date does not make the claim itself wrong, and rejecting it would cut a person off from their claim record over a gap in old data. The "filed after birth" check is skipped for these claims because there is nothing to compare against. Staff can see every affected claim in the report and fill in the date.

**2. The same line appears twice.**
*Decision:* Load the first copy and skip the second, reporting it as "exact duplicate of line N."
*Why:* An identical line adds no information. Loading it once gives the correct result, and reporting the copy lets the mainframe team see that the export repeated a record.

**3. The same claim ID appears twice with different values.**
*Decision:* Load neither version. Both lines go in the rejection report, each pointing at the other, marked "held for agency review."
*Why:* The file does not say which version is current. In the sample, one says Open and the other says Denied. Guessing wrong could show the wrong decision on someone's benefits. It is safer to hold the claim until someone who knows the case confirms the correct version.

**4. The trailer count disagrees with the number of records.**
*Decision:* Load the records that are in the file and add a warning stating both numbers.
*Why:* Refusing the whole file would block every good record over a count problem. The warning makes the mismatch impossible to miss, so someone can check whether records were lost in the export.

---

## 3. Deferred at the time limit

- **Import history.** Each summary is returned but not saved. Next step: store summaries in an `imports` collection so the rejection report can be retrieved later.
- **Held-for-review workflow.** Conflicting claims are reported but there is no endpoint to resolve them.
- **Integration test against real MongoDB.** API tests use an in-memory store. Next step: add Testcontainers so `mvn verify` runs against a MongoDB container.
- **Large files.** The file is read into memory; fine for a nightly export of this size, but a streaming reader and batched writes would be needed for millions of rows.
- **Security.** No authentication on the API. Import endpoints in particular should be restricted.
- **Removing claims.** A claim dropped from a later export stays in the database; the agency needs to say whether that means "closed" or "deleted."
- **Header export-date check.** A filed date later than the export date is not yet rejected (this is the live-extension task).

---

## 4. Plan for deploying on AWS

Run the container on **Amazon ECS with Fargate**, because it runs the existing Docker image without managing servers and scales the API independently. Use **Amazon DocumentDB** (MongoDB-compatible) as the database so the agency gets managed backups, encryption, and patching; a hosted **MongoDB Atlas** cluster on AWS is the alternative if full MongoDB compatibility matters more than staying inside AWS. The mainframe drops each nightly export into an **S3** bucket, and an **EventBridge** rule triggers the import so no one has to run it by hand. Store the connection string in **Secrets Manager**, put the API behind an **Application Load Balancer** in private subnets, and send logs and rejection counts to **CloudWatch** with an alarm when rejections spike. **Main risk:** DocumentDB is not 100% MongoDB-compatible, so the integration tests must run against DocumentDB before go-live.

---

## 5. Question for the agency's system owner

When the same claim ID appears twice in one export with different values, is the later line in the file always the most recent update, or can the order not be trusted?

The answer decides whether decision 3 can safely load the later version automatically instead of holding both for review.
