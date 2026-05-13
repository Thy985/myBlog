package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.UserCreateDTO;
import com.xingchen.backend.entity.*;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.impl.AdminServiceImpl;
import com.xingchen.backend.vo.UserAdminVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl Tests")
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private BlogSettingMapper blogSettingMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setNickname("Test User " + id);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreatedTime(LocalDateTime.now());
        user.setRegisterTime(LocalDateTime.now());
        return user;
    }

    private Article createArticle(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("Article " + id);
        article.setCreatedTime(LocalDateTime.now());
        return article;
    }

    @Nested
    @DisplayName("getDashboardData()")
    class GetDashboardDataTests {

        @Test
        @DisplayName("should return dashboard data with counts")
        void getDashboardData_success() {
            when(userMapper.countAll()).thenReturn(100L);
            when(articleMapper.countAll()).thenReturn(50L);
            when(commentMapper.countAll()).thenReturn(200L);

            List<Article> recentArticles = Arrays.asList(createArticle(1L), createArticle(2L));
            when(articleMapper.selectRecentArticles(5)).thenReturn(recentArticles);

            Map<String, Object> result = adminService.getDashboardData();

            assertNotNull(result);
            assertEquals(100L, result.get("userCount"));
            assertEquals(50L, result.get("articleCount"));
            assertEquals(200L, result.get("commentCount"));
            assertNotNull(result.get("recentArticles"));
        }
    }

    @Nested
    @DisplayName("getUserList()")
    class GetUserListTests {

        @Test
        @DisplayName("should return paginated user list")
        void getUserList_success() {
            List<User> users = Arrays.asList(
                    createTestUser(1L, "user1"),
                    createTestUser(2L, "user2")
            );
            when(userMapper.selectByPage(isNull(), eq("1"), eq(0), eq(10)))
                    .thenReturn(users);

            List<UserAdminVO> result = adminService.getUserList(1, 10, null, "active");

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list when no users")
        void getUserList_empty_returnsEmpty() {
            when(userMapper.selectByPage(isNull(), isNull(), eq(0), eq(10)))
                    .thenReturn(Collections.emptyList());

            List<UserAdminVO> result = adminService.getUserList(1, 10, null, null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("countUsers()")
    class CountUsersTests {

        @Test
        @DisplayName("should return user count with keyword filter")
        void countUsers_withKeyword_returnsCount() {
            when(userMapper.countByCondition("test", "1")).thenReturn(5L);

            long count = adminService.countUsers("test", "active");

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("should return all users count when no filter")
        void countUsers_noFilter_returnsAll() {
            when(userMapper.countByCondition(isNull(), isNull())).thenReturn(100L);

            long count = adminService.countUsers(null, null);

            assertEquals(100L, count);
        }
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("should create user successfully")
        void createUser_success() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setEmail("new@example.com");
            dto.setNickname("New User");

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            UserAdminVO result = adminService.createUser(dto);

            assertNotNull(result);
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when username exists")
        void createUser_usernameExists_throwsException() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("existinguser");
            dto.setPassword("password123");

            when(userMapper.selectByUsername("existinguser")).thenReturn(createTestUser(1L, "existinguser"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminService.createUser(dto));

            assertEquals(ErrorCode.USER_ALREADY_EXISTS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateUserStatus()")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("should update user status successfully")
        void updateUserStatus_success() {
            User user = createTestUser(1L, "testuser");
            when(userMapper.selectOneById(1L)).thenReturn(user);
            when(userMapper.update(any(User.class))).thenReturn(1);

            assertDoesNotThrow(() -> adminService.updateUserStatus(1L, "disabled"));

            verify(userMapper).update(argThat(u -> u.getUpdatedTime() != null));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void updateUserStatus_notFound_throwsException() {
            when(userMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminService.updateUserStatus(999L, "disabled"));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("should soft delete user successfully")
        void deleteUser_success() {
            User user = createTestUser(1L, "testuser");
            when(userMapper.selectOneById(1L)).thenReturn(user);
            when(userMapper.update(any(User.class))).thenReturn(1);

            assertDoesNotThrow(() -> adminService.deleteUser(1L));

            verify(userMapper).update(argThat(u -> u.getIsDeleted() == 1));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void deleteUser_notFound_throwsException() {
            when(userMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminService.deleteUser(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("assignRoles()")
    class AssignRolesTests {

        @Test
        @DisplayName("should assign roles successfully")
        void assignRoles_success() {
            User user = createTestUser(1L, "testuser");
            when(userMapper.selectOneById(1L)).thenReturn(user);
            when(userRoleMapper.deleteByUserId(1L)).thenReturn(1);

            List<Long> roleIds = Arrays.asList(1L, 2L);

            assertDoesNotThrow(() -> adminService.assignRoles(1L, roleIds));

            verify(userRoleMapper).deleteByUserId(1L);
            verify(userRoleMapper, times(2)).insert(any(UserRole.class));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void assignRoles_userNotFound_throwsException() {
            when(userMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminService.assignRoles(999L, Arrays.asList(1L)));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
