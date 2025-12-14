# Tests Analysis

**Tools**

- **Test framework:** JUnit 5 (executed via Maven Surefire)
- **Coverage:** JaCoCo (HTML report under `target/site/jacoco`)

**Unit Test Scope**

- Backend only (controllers, utils, domain model). Frontend UI is excluded.

**Coverage Notes**

- Getters/setters and some simple constructors are not fully covered by unit tests.
- Coverage percentages below refer to JaCoCo instruction coverage for each class.
- Overall totals include all packages and tests executed together.

**Totals**

- **Instruction Coverage:** 86%
- **Branch Coverage:** 72%

---

## Coverages

| Package                       | Class                    | Unit Coverage | Test Class                   |
| ----------------------------- | ------------------------ | ------------- | ---------------------------- |
| `com.agile.projet.controller` | `ApiController`          | 72%           | `ApiControllerTest`          |
| `com.agile.projet.controller` | `Controller`             | 91%           | `ControllerTest`             |
| `com.agile.projet.utils`      | `CalculPlusCoursChemins` | 98%           | `CalculPlusCoursCheminsTest` |
| `com.agile.projet.utils`      | `CalculTSP`              | 86%           | —                            |
| `com.agile.projet.utils`      | `MatriceChemins`         | 95%           | —                            |
| `com.agile.projet.utils`      | `MatriceCout`            | 100%          | —                            |
| `com.agile.projet.utils`      | `NodePair`               | 92%           | —                            |
| `com.agile.projet.utils`      | `XmlPlanParser`          | 93%           | —                            |
| `com.agile.projet.utils`      | `XmlDeliveryParser`      | 100%          | `XmlDeliveryParserTest`      |
| `com.agile.projet.model`      | `PickupDeliveryModel`    | 92%           | —                            |
| `com.agile.projet.model`      | `Plan`                   | 76%           | —                            |
| `com.agile.projet.model`      | `Noeud`                  | 73%           | —                            |
| `com.agile.projet.model`      | `Troncon`                | 85%           | —                            |
| `com.agile.projet.model`      | `Entrepot`               | 72%           | —                            |
| `com.agile.projet.model`      | `Delivery`               | 72%           | —                            |
| `com.agile.projet.model`      | `DemandeDelivery`        | 92%           | —                            |
| `com.agile.projet.model`      | `Tournee`                | 72%           | —                            |
| `com.agile.projet.model`      | `Tournee.Etape`          | 46%           | —                            |

> Test classes detected (from `target/surefire-reports`):
>
> - `com.agile.projet.controller.ApiControllerTest`
> - `com.agile.projet.controller.ControllerTest`
> - `com.agile.projet.utils.CalculPlusCoursCheminsTest`
> - `com.agile.projet.utils.XmlDeliveryParserTest`

---

## How to Recompute Coverage

```powershell
# From the backend project root
cd "C:\Users\amine chakroun\Desktop\PickUp-Delivery\backend\projet"
./mvnw clean test
./mvnw jacoco:report
# Open HTML report
Start-Process "C:\Users\amine chakroun\Desktop\PickUp-Delivery\backend\projet\target\site\jacoco\index.html"
```
