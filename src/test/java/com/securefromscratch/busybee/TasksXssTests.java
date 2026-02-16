// package com.securefromscratch.busybee;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.web.servlet.MockMvc;

// import java.util.List;

// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest
// @AutoConfigureMockMvc
// class CreateTaskXssTests {

//     @Autowired MockMvc mvc;
//     @Autowired ObjectMapper om;

//     private String createJson(String name, String desc) throws Exception {
//         // users that exist in your project: Yariv (ADMIN), Or (CREATOR), Eyal (TRIAL)
//         // var body = new CreateBody(
//         //         name,
//         //         desc,
//         //         null,   // dueDate
//         //         null,   // dueTime
//         //         List.of("Yariv") // responsibilityOf must be existing users
//         // );
//         var body = new CreateBody(
//         name,
//         desc,
//         null,   // dueDate
//         null,   // dueTime
//         List.of("Yariv")
//         );
//         return om.writeValueAsString(body);
//     }

//     // local DTO for tests only
//     record CreateBody(
//             String name,
//             String desc,
//             String dueDate,
//             String dueTime,
//             List<String> responsibilityOf
//     ) {}

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_allowsSafeDescWithLinksImagesAndFormatting() throws Exception {
//         String desc = """
//                 line1
//                 <b>bold</b> <i>italic</i> <u>u</u>
//                 <a href="https://example.com">link</a>
//                 <img src="uploads/pic.png">
//                 """;

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-safe-desc-1", desc)))
//                 .andExpect(status().isCreated());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsScriptTagXss() throws Exception {
//         String desc = "<script>alert(1)</script>";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-xss-script", desc)))
//                 .andExpect(status().isBadRequest());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsJavascriptSchemeXss() throws Exception {
//         String desc = "<a href=\"javascript:alert(1)\">x</a>";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-xss-js-scheme", desc)))
//                 .andExpect(status().isBadRequest());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsOnEventHandlerXss() throws Exception {
//         String desc = "<img src=\"uploads/a.png\" onerror=\"alert(1)\">";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-xss-onerror", desc)))
//                 .andExpect(status().isBadRequest());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsDataUriInImgSrc() throws Exception {
//         String desc = "<img src=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==\">";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-xss-data-uri", desc)))
//                 .andExpect(status().isBadRequest());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsUnknownHtmlTag() throws Exception {
//         String desc = "<div>not allowed</div>";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson("task-xss-unknown-tag", desc)))
//                 .andExpect(status().isBadRequest());
//     }

//     @Test
//     @WithMockUser(username = "Or", roles = {"CREATOR"})
//     void create_rejectsHtmlInNameBecauseOfPattern() throws Exception {
//         String desc = "ok";
//         String name = "<b>bad</b>";

//         mvc.perform(post("/create")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(createJson(name, desc)))
//                 .andExpect(status().isBadRequest());
//     }
// }
package com.securefromscratch.busybee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest
@AutoConfigureMockMvc
class TasksXssTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        Path p = tempDir.resolve("tasks-test.dat");
        registry.add("busybee.tasks.file", () -> p.toString());
        System.out.println(">>> Using isolated tasks file for tests: " + p);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String createJson(String name, String desc) throws Exception {
        var body = new CreateBody(
                name,
                desc,
                null,   // dueDate
                null,   // dueTime
                List.of("Yariv")
        );
        return om.writeValueAsString(body);
    }

    // local DTO for tests only
    record CreateBody(
            String name,
            String desc,
            String dueDate,
            String dueTime,
            List<String> responsibilityOf
    ) {}

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_allowsSafeDescWithLinksImagesAndFormatting() throws Exception {
        System.out.println("\n>>> TEST START: create_allowsSafeDescWithLinksImagesAndFormatting");

        String desc = """
                line1
                <b>bold</b> <i>italic</i> <u>u</u>
                <a href="https://example.com">link</a>
                <img src="uploads/pic.png">
                """;

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-safe-desc-1", desc)))
                .andExpect(status().isCreated());

        System.out.println("<<< TEST PASSED: create_allowsSafeDescWithLinksImagesAndFormatting\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsScriptTagXss() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsScriptTagXss");

        String desc = "<script>alert(1)</script>";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-xss-script", desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsScriptTagXss\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsJavascriptSchemeXss() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsJavascriptSchemeXss");

        String desc = "<a href=\"javascript:alert(1)\">x</a>";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-xss-js-scheme", desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsJavascriptSchemeXss\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsOnEventHandlerXss() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsOnEventHandlerXss");

        String desc = "<img src=\"uploads/a.png\" onerror=\"alert(1)\">";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-xss-onerror", desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsOnEventHandlerXss\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsDataUriInImgSrc() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsDataUriInImgSrc");

        String desc = "<img src=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==\">";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-xss-data-uri", desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsDataUriInImgSrc\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsUnknownHtmlTag() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsUnknownHtmlTag");

        String desc = "<div>not allowed</div>";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("task-xss-unknown-tag", desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsUnknownHtmlTag\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsHtmlInNameBecauseOfPattern() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsHtmlInNameBecauseOfPattern");

        String desc = "ok";
        String name = "<b>bad</b>";

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(name, desc)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsHtmlInNameBecauseOfPattern\n");
    }

    // --------------------------------------------------------------------
    // ADDITIONAL TESTS (for create סעיפים ה/ו/ז/ח/י)
    // --------------------------------------------------------------------

    private String createJsonFull(String name, String desc, String dueDate, String dueTime, List<String> responsibilityOf) throws Exception {
        var body = new CreateBody(name, desc, dueDate, dueTime, responsibilityOf);
        return om.writeValueAsString(body);
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsDueTimeWithoutDueDate() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsDueTimeWithoutDueDate");

        String desc = "ok";
        String dueDate = null;
        String dueTime = "10:00:00";

        List<String> resp = List.of("Yariv");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-dueTime-no-date", desc, dueDate, dueTime, resp)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsDueTimeWithoutDueDate\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsPastDueDate() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsPastDueDate");

        String desc = "ok";
        String dueDate = LocalDate.now().minusDays(1).toString();
        String dueTime = null;
        List<String> resp = List.of("Yariv");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-past-date", desc, dueDate, dueTime, resp)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsPastDueDate\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsPastDueDateTimeSameDay() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsPastDueDateTimeSameDay");

        String desc = "ok";
        String dueDate = LocalDate.now().toString();
        // take a time safely in the past
        String dueTime = LocalTime.now().minusMinutes(5).withSecond(0).withNano(0).toString();
        List<String> resp = List.of("Yariv");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-past-datetime", desc, dueDate, dueTime, resp)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsPastDueDateTimeSameDay\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_allowsDueDateTodayWithoutTime() throws Exception {
        System.out.println("\n>>> TEST START: create_allowsDueDateTodayWithoutTime");

        String desc = "ok";
        String dueDate = LocalDate.now().toString();
        String dueTime = null;
        List<String> resp = List.of("Yariv");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-today-date-ok", desc, dueDate, dueTime, resp)))
                .andExpect(status().isCreated());

        System.out.println("<<< TEST PASSED: create_allowsDueDateTodayWithoutTime\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsResponsibilityOfNonExistingUser() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsResponsibilityOfNonExistingUser");

        String desc = "ok";
        List<String> resp = List.of("ThisUserDoesNotExist123");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-bad-resp-nonexist", desc, null, null, resp)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsResponsibilityOfNonExistingUser\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_rejectsResponsibilityOfBadFormat() throws Exception {
        System.out.println("\n>>> TEST START: create_rejectsResponsibilityOfBadFormat");

        String desc = "ok";
        // must start with a letter; this starts with digit
        List<String> resp = List.of("1BadName");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-bad-resp-format", desc, null, null, resp)))
                .andExpect(status().isBadRequest());

        System.out.println("<<< TEST PASSED: create_rejectsResponsibilityOfBadFormat\n");
    }

    @Test
    @WithMockUser(username = "Or", roles = {"CREATOR"})
    void create_returns409OnDuplicateName() throws Exception {
        System.out.println("\n>>> TEST START: create_returns409OnDuplicateName");

        String name = "task-dup-409";
        String desc = "ok";

        // first create -> 201
        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull(name, desc, null, null, List.of("Yariv"))))
                .andExpect(status().isCreated());

        // second create -> 409
        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull(name, desc, null, null, List.of("Yariv"))))
                .andExpect(status().isConflict());

        System.out.println("<<< TEST PASSED: create_returns409OnDuplicateName\n");
    }

    @Test
    @WithMockUser(username = "AdminUser", roles = {"ADMIN"})
    void create_adminCanAlwaysCreate() throws Exception {
        System.out.println("\n>>> TEST START: create_adminCanAlwaysCreate");

        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-admin-create", "ok", null, null, List.of("Yariv"))))
                .andExpect(status().isCreated());

        System.out.println("<<< TEST PASSED: create_adminCanAlwaysCreate\n");
    }


    @Test
    @WithMockUser(username = "Eyal", roles = {"TRIAL"})
    void create_trialDeniedWhenHasOpenTask() throws Exception {
        System.out.println("\n>>> TEST START: create_trialDeniedWhenHasOpenTask");

        // first create (open task) -> 201
        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-trial-open-1", "ok", null, null, List.of("Yariv"))))
                .andExpect(status().isCreated());

        // second create while first is still open -> should be forbidden by isAuthorizedToCreate
        mvc.perform(post("/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonFull("task-trial-open-2", "ok", null, null, List.of("Yariv"))))
                .andExpect(status().isForbidden());

        System.out.println("<<< TEST PASSED: create_trialDeniedWhenHasOpenTask\n");
    }
}
