package at.jku.dke.task_app.owl.controllers;

import at.jku.dke.etutor.task_app.auth.AuthConstants;
import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.owl.ClientSetupExtension;
import at.jku.dke.task_app.owl.DatabaseSetupExtension;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskGroupRepository;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({DatabaseSetupExtension.class, ClientSetupExtension.class})
class TaskControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OWLTaskRepository repository;

    @Autowired
    private OWLTaskGroupRepository groupRepository;

    private long taskId;
    private long taskGroupId;

    @BeforeEach
    void initDb() {
        this.repository.deleteAll();

        this.taskId = this.repository.save(new OWLTask(1L, BigDecimal.TWO, TaskStatus.APPROVED, "Class: Person\nSubClassOf: Human\nDisjointWith: Animal", "Person=3, Human=2, Animal=1", 1, 1)).getId();
    }

    //#region --- GET ---
    @Test
    void getShouldReturnOk() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .accept(ContentType.JSON)
            // WHEN
            .when()
            .get("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("solution", equalTo("Class: Person\nSubClassOf: Human\nDisjointWith: Animal"));
    }

    @Test
    void getShouldReturnNotFound() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .accept(ContentType.JSON)
            // WHEN
            .when()
            .get("/api/task/{id}", this.taskId + 1)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(404);
    }

    @Test
    void getShouldReturnForbidden() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.SUBMIT_API_KEY)
            .accept(ContentType.JSON)
            // WHEN
            .when()
            .get("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(403);
    }
    //#endregion

    //#region --- CREATE ---
    @Test
    void createShouldReturnCreated() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Student\nSubClassOf: Person\nDisjointWith: Teacher", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .post("/api/task/{id}", this.taskId + 2)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header("Location", containsString("/api/task/" + (this.taskId + 2)))
            .body("descriptionDe", any(String.class))
            .body("descriptionEn", any(String.class));
    }

    @Test
    void createShouldReturnBadRequestOnInvalidBody() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Student\nSubClassOf: Person\nDisjointWith: Teacher", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .post("/api/task/{id}", this.taskId + 2)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(400);
    }

    @Test
    void createShouldReturnBadRequestOnEmptyBody() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            // WHEN
            .when()
            .post("/api/task/{id}", this.taskId + 2)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(400);
    }

    @Test
    void createShouldReturnForbidden() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.SUBMIT_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Student\nSubClassOf: Person\nDisjointWith: Teacher", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .post("/api/task/{id}", this.taskId + 2)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(403);
    }
    //#endregion

    //#region --- UPDATE ---
    @Test
    void updateShouldReturnOk() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("\"Class: Apple\\nSubClassOf: Fruit\\nDisjointWith: Orange\"", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .put("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("descriptionDe", any(String.class))
            .body("descriptionEn", any(String.class));
    }

    @Test
    void updateShouldReturnNotFound() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "OWL", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .put("/api/task/{id}", this.taskId + 1)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(404);
    }

    @Test
    void updateShouldReturnBadRequestOnInvalidBody() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "sql", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .put("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(400);
    }

    @Test
    void updateShouldReturnBadRequestOnEmptyBody() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            .contentType(ContentType.JSON)
            // WHEN
            .when()
            .put("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(400);
    }

    @Test
    void updateShouldReturnForbidden() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.SUBMIT_API_KEY)
            .contentType(ContentType.JSON)
            .body(new ModifyTaskDto<>(this.taskGroupId, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Person=3, Human=2, Animal=1", 1, 1)))
            // WHEN
            .when()
            .put("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(403);
    }
    //#endregion

    //#region --- DELETE ---
    @Test
    void deleteShouldReturnNoContent() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            // WHEN
            .when()
            .delete("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(204);
    }

    @Test
    void deleteShouldReturnNoContentOnNotFound() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.CRUD_API_KEY)
            // WHEN
            .when()
            .delete("/api/task/{id}", this.taskId + 1)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(204);
    }

    @Test
    void deleteShouldReturnForbidden() {
        given()
            .port(port)
            .header(AuthConstants.AUTH_TOKEN_HEADER_NAME, ClientSetupExtension.SUBMIT_API_KEY)
            // WHEN
            .when()
            .delete("/api/task/{id}", this.taskId)
            // THEN
            .then()
            .log().ifValidationFails()
            .statusCode(403);
    }
    //#endregion

    @Test
    void mapToDto() {
        // Arrange
        var task = new OWLTask("Class: Car\nSubClassOf: Vehicle\nDisjointWith: Bike", "Person=3, Human=2, Animal=1", 1, 1);

        // Act
        var result = new TaskController(null).mapToDto(task);

        // Assert
        assertEquals("Class: Car\nSubClassOf: Vehicle\nDisjointWith: Bike", result.solution());
    }

}
