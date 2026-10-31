package com.campus.events.controller;

import com.campus.events.dto.UserRegistrationDto;
import com.campus.events.model.AppUser;
import com.campus.events.model.Role;
import com.campus.events.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("Anonymous user cannot access student dashboard and is redirected to login")
    void testAnonymousAccessStudentDashboardRedirects() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Anonymous user cannot access administrator dashboard and is redirected to login")
    void testAnonymousAccessAdminDashboardRedirects() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "student@campus.edu", roles = {"STUDENT"})
    @DisplayName("Student user is forbidden from accessing administrator dashboard (403 Access Denied)")
    void testStudentCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@campus.edu", roles = {"ADMIN"})
    @DisplayName("Admin user can access administrator dashboard successfully (200 OK)")
    void testAdminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @DisplayName("Public registration submits successfully with CSRF token")
    void testPublicRegistrationWithCsrf() throws Exception {
        AppUser savedStudent = new AppUser("New Student", "new.student@campus.edu", "hashed", Role.STUDENT);
        when(userService.registerStudent(any(UserRegistrationDto.class))).thenReturn(savedStudent);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "New Student")
                        .param("email", "new.student@campus.edu")
                        .param("password", "Secret@123")
                        .param("confirmPassword", "Secret@123")
                        .param("studentIdNumber", "STU-999")
                        .param("department", "Information Technology"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService).registerStudent(any(UserRegistrationDto.class));
    }

    @Test
    @WithMockUser(username = "organizer@campus.edu", roles = {"ORGANIZER"})
    @DisplayName("Organizer role cannot access global admin dashboard (403 Access Denied)")
    void testOrganizerCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@campus.edu", roles = {"STUDENT"})
    @DisplayName("Student role cannot access organizer portal (403 Access Denied)")
    void testStudentCannotAccessOrganizerDashboard() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public certificate verification endpoint is accessible to anonymous users (200 OK)")
    void testPublicCertificateVerificationAccessible() throws Exception {
        mockMvc.perform(get("/verify-certificate"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-certificate"));
    }
}

