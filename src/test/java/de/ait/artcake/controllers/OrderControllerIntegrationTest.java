package de.ait.artcake.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ait.artcake.dto.NewOrderDto;
import de.ait.artcake.dto.OrderDto;
import de.ait.artcake.dto.OrderInProcessDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("OrdersController is works: ")
@ActiveProfiles("test")
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/orders/cakes/ method is works: ")
    class AddOrderTest {
        @WithUserDetails(value = "client@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void addOrderAsAuthenticatedClient() throws Exception {

            String body = objectMapper.writeValueAsString(NewOrderDto.builder()
                    .clientWishes("Make in blue and white colours")
                    .count(1)
                    .deadline("2025-10-10")
                    .build());

            mockMvc.perform(post("/api/orders/cakes/1")
                            .param("cakeId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.count", is(1)))
                    .andExpect(jsonPath("$.clientWishes", is("Make in blue and white colours")))
                    .andExpect(jsonPath("$.totalPrice", is(33.33)))
                    .andExpect(jsonPath("$.creationDate", is("2024-01-28")))
                    .andExpect(jsonPath("$.deadline", is("2025-10-10")))
                    .andExpect(jsonPath("$.state", is("CREATED")));
        }

        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void addOrderAsNotAuthenticatedClient() throws Exception {

            String body = objectMapper.writeValueAsString(NewOrderDto.builder()
                    .clientWishes("Make in blue and white colours")
                    .count(2)
                    .deadline("2025-10-10")
                    .build());

            mockMvc.perform(post("/api/orders/cakes/1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{order-id} method is works:")
    class AddOrderToProcess {
        @WithUserDetails(value = "manager@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void move_order_to_process_as_Manager() throws Exception {

            String body = objectMapper.writeValueAsString(OrderInProcessDto.builder()
                    .confectionerId(1L)
                    .extra(5.50)
                    .build());

            mockMvc.perform(put("/api/orders/1")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.count", is(1)))
                    .andExpect(jsonPath("$.clientWishes", is("For birthday(30 years)")))
                    .andExpect(jsonPath("$.creationDate", is("2023-09-01")))
                    .andExpect(jsonPath("$.deadline", is("2023-10-10")))
                    .andExpect(jsonPath("$.state", is("IN_PROCESS")))
                    .andExpect(jsonPath("$.totalPrice", is(206.0)));
        }

    }

    @Nested
    @DisplayName("PUT /api/orders/{order-id} method is works: ")
    class GetUsersByRoleTests {
        @WithUserDetails(value = "confectioner@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void orderIsFinishedAsConfectioner() throws Exception {

            String body = objectMapper.writeValueAsString(OrderDto.builder()
                    .state("IN_PROCESS")
                    .build());

            mockMvc.perform(put("/api/orders/1/done")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state", is("FINISHED")));
        }

        @WithUserDetails(value = "manager@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void orderIsFinishedAsManagerIsForbidden() throws Exception {

            String body = objectMapper.writeValueAsString(OrderDto.builder()
                    .state("IN_PROCESS")
                    .build());

            mockMvc.perform(put("/api/orders/1/done")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @WithUserDetails(value = "confectioner@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void orderCantFinishAsConfectioner() throws Exception {

            String body = objectMapper.writeValueAsString(OrderDto.builder()
                    .state("IN_PROCESS")
                    .build());

            mockMvc.perform(put("/api/orders/1/decline")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state", is("IN_PROCESS")));
        }

        @WithUserDetails(value = "manager@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void orderCantFinishedAsManagerIsForbidden() throws Exception {

            String body = objectMapper.writeValueAsString(OrderDto.builder()
                    .state("IN_PROCESS")
                    .build());

            mockMvc.perform(put("/api/orders/1/decline")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/client/orders method is works:")
    class MoveOrderstoProcess {
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void move_order_to_process_as_Unauthorized() throws Exception {

            String body = objectMapper.writeValueAsString(OrderInProcessDto.builder()
                    .confectionerId(1L)
                    .extra(5.50)
                    .build());

            mockMvc.perform(put("/api/orders/1")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @WithUserDetails(value = "client@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void move_order_to_process_as_client_is_Forbidden() throws Exception {

            String body = objectMapper.writeValueAsString(OrderInProcessDto.builder()
                    .confectionerId(1L)
                    .extra(5.50)
                    .build());

            mockMvc.perform(put("/api/orders/1")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @WithUserDetails(value = "confectioner@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void move_order_to_process_as_confectioner_is_Forbidden() throws Exception {

            String body = objectMapper.writeValueAsString(OrderInProcessDto.builder()
                    .confectionerId(1L)
                    .extra(5.50)
                    .build());

            mockMvc.perform(put("/api/orders/1")
                            .param("orderId", "1")
                            .header("Content-Type", "application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/client/orders method is works:")
    class GetAllOrdersForClient {

        @WithUserDetails(value = "client@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_for_Client_as_Client() throws Exception {

            mockMvc.perform(get("/api/users/client/orders")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(1)));
        }

        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_for_Client_as_Unauthorized() throws Exception {

            mockMvc.perform(get("/api/users/client/orders")
                            .param("page", "0"))
                    .andExpect(status().isUnauthorized());
        }

        @WithUserDetails(value = "confectioner@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_for_Client_as_Confectioner() throws Exception {

            mockMvc.perform(get("/api/users/client/orders")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(0)));;
        }

        @WithUserDetails(value = "manager@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_for_Client_as_Manager() throws Exception {

            mockMvc.perform(get("/api/users/client/orders")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(1)));;
        }
    }

    @Nested
    @DisplayName("PUT /api/users/confectioner/orders method is works:")
    class GetAllOrdersToDoForConfectioner {
        @WithUserDetails(value = "confectioner@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_to_do_for_Confectioner_as_Confectioner() throws Exception {

            mockMvc.perform(get("/api/users/confectioner/orders")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(2)));;
        }

        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_to_do_for_Confectioner_as_Unauthorized() throws Exception {

            mockMvc.perform(get("/api/users/confectioner/orders")
                            .param("page", "0"))
                    .andExpect(status().isUnauthorized());
        }

        @WithUserDetails(value = "client@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_to_do_for_Confectioner_as_Client() throws Exception {

            mockMvc.perform(get("/api/users/confectioner/orders")
                            .param("page", "0"))
                    .andExpect(status().isForbidden());
        }

        @WithUserDetails(value = "manager@mail.com")
        @Sql(scripts = "/sql/data_for_orders.sql")
        @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
        @Test
        void get_all_orders_to_do_for_Confectioner_as_Manager() throws Exception {

            mockMvc.perform(get("/api/users/confectioner/orders")
                            .param("page", "0"))
                    .andExpect(status().isForbidden());
        }
    }
}