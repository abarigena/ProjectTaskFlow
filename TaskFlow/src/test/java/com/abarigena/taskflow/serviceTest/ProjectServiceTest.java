package com.abarigena.taskflow.serviceTest;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.ProjectMapper;
import com.abarigena.taskflow.mapper.UserMapper;
import com.abarigena.taskflow.serviceSQL.ProjectServiceImpl;
import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.entity.User;
import com.abarigena.taskflow.storeSQL.repository.ProjectRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
@DisplayName("Unit тесты для ProjectServiceImpl")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Long projectId;
    private Long ownerId;
    private Project projectEntity; // Существующая Entity (для find, update, delete)
    private ProjectDto projectDto; // Существующий DTO

    private ProjectDto newProjectDto; // Входящий DTO для создания
    private Project projectEntityToSave; // Entity из DTO для сохранения
    private Project savedProjectEntity; // Сохраненная Entity с ID
    private ProjectDto savedProjectDto; // DTO для возврата после создания

    private Long existingProjectId; // ID существующего проекта для обновления
    private ProjectDto updateProjectDto; // Входящий DTO для обновления
    private Project existingProjectEntity; // Существующая Entity до обновления
    private Project updatedProjectEntity; // Entity после сохранения
    private ProjectDto updatedProjectDto; // DTO для возврата после обновления

    private Long participantUserId;
    private User participantUserEntity;
    private UserDto participantUserDto;


    @BeforeEach
    void setUp() {
        // Инициализация данных-заглушек для find (getProjectById)
        projectId = 1L;
        ownerId = 100L;
        projectEntity = Project.builder()
                .id(projectId).name("Тестовый Проект").description("Описание тестового проекта")
                .status(Project.Status.ACTIVE).ownerId(ownerId)
                .createdAt(LocalDateTime.now().minusDays(1)).updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        projectDto = ProjectDto.builder()
                .id(projectId).name("Тестовый Проект DTO").description("Описание тестового проекта DTO")
                .status(Project.Status.ACTIVE).ownerId(ownerId)
                .createdAt(projectEntity.getCreatedAt()).updatedAt(projectEntity.getUpdatedAt())
                .build();

        // Инициализация данных-заглушек для createProject
        Long newOwnerId = 200L;
        newProjectDto = ProjectDto.builder()
                .name("Новый Проект").description("Описание нового проекта").ownerId(newOwnerId)
                .build();
        projectEntityToSave = Project.builder()
                .name(newProjectDto.getName()).description(newProjectDto.getDescription()).ownerId(newOwnerId)
                .status(Project.Status.ACTIVE).build(); // Сервис устанавливает дефолт
        savedProjectEntity = Project.builder()
                .id(10L).name(newProjectDto.getName()).description(newProjectDto.getDescription()).ownerId(newOwnerId)
                .status(Project.Status.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        savedProjectDto = ProjectDto.builder()
                .id(savedProjectEntity.getId()).name(savedProjectEntity.getName()).description(savedProjectEntity.getDescription()).ownerId(savedProjectEntity.getOwnerId())
                .status(savedProjectEntity.getStatus()).createdAt(savedProjectEntity.getCreatedAt()).updatedAt(savedProjectEntity.getUpdatedAt())
                .build();

        // Инициализация данных-заглушек для updateProject
        existingProjectId = projectId; // Обновляем существующий проект
        Long anotherOwnerId = 300L;
        updateProjectDto = ProjectDto.builder()
                .name("Обновленный Проект").status(Project.Status.COMPLETED).ownerId(anotherOwnerId)
                .build();

        existingProjectEntity = projectEntity; // Существующая сущность до обновления

        updatedProjectEntity = Project.builder() // Сущность после сохранения
                .id(existingProjectId).name(updateProjectDto.getName()).description(existingProjectEntity.getDescription())
                .status(updateProjectDto.getStatus()).ownerId(updateProjectDto.getOwnerId())
                .createdAt(existingProjectEntity.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        updatedProjectDto = ProjectDto.builder() // DTO для возврата
                .id(updatedProjectEntity.getId()).name(updatedProjectEntity.getName()).description(updatedProjectEntity.getDescription())
                .status(updatedProjectEntity.getStatus()).ownerId(updatedProjectEntity.getOwnerId())
                .createdAt(updatedProjectEntity.getCreatedAt()).updatedAt(updatedProjectEntity.getUpdatedAt())
                .build();

        participantUserId = 400L;
        participantUserEntity = User.builder().id(participantUserId).firstName("Участник").build();
        participantUserDto = UserDto.builder().id(participantUserId).firstName("Участник DTO").build();
    }

    // --- Тесты для getProjectById ---
    @Test
    @DisplayName("getProjectById - должен успешно вернуть проект по существующему ID")
    void getProjectById_ExistingId_ShouldReturnProject() {
        when(projectRepository.findById(projectId)).thenReturn(Mono.just(projectEntity));
        when(projectMapper.toDto(projectEntity)).thenReturn(projectDto);
        StepVerifier.create(projectService.getProjectById(projectId)).expectNext(projectDto).verifyComplete();
        verify(projectRepository).findById(projectId);
        verify(projectMapper).toDto(projectEntity);
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("getProjectById - должен вернуть ошибку ResourceNotFoundException для несуществующего ID")
    void getProjectById_NonExistingId_ShouldReturnError() {
        Long nonExistingId = 999L;
        when(projectRepository.findById(nonExistingId)).thenReturn(Mono.empty());
        StepVerifier.create(projectService.getProjectById(nonExistingId)).expectError(ResourceNotFoundException.class).verify();
        verify(projectRepository).findById(nonExistingId);
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    // --- Тесты для createProject ---
    @Test
    @DisplayName("createProject - должен успешно создать проект, когда владелец существует")
    void createProject_OwnerExists_ShouldCreateProject() {
        Long newOwnerId = newProjectDto.getOwnerId();
        when(projectMapper.toEntity(newProjectDto)).thenReturn(projectEntityToSave);
        when(userRepository.findById(newOwnerId)).thenReturn(Mono.just(User.builder().id(newOwnerId).build()));
        when(projectRepository.save(projectEntityToSave)).thenReturn(Mono.just(savedProjectEntity));
        when(projectMapper.toDto(savedProjectEntity)).thenReturn(savedProjectDto);
        StepVerifier.create(projectService.createProject(newProjectDto)).expectNext(savedProjectDto).verifyComplete();
        verify(projectMapper).toEntity(newProjectDto);
        verify(userRepository).findById(newOwnerId);
        verify(projectRepository).save(projectEntityToSave);
        verify(projectMapper).toDto(savedProjectEntity);
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("createProject - должен вернуть ошибку ResourceNotFoundException, если владелец не найден")
    void createProject_OwnerNotFound_ShouldReturnError() {
        Long nonExistingOwnerId = 999L;
        ProjectDto newProjectDtoWithNonExistingOwner = ProjectDto.builder().name("Проект без владельца").ownerId(nonExistingOwnerId).build();
        Project projectEntityToSaveWithNonExistingOwner = Project.builder().name(newProjectDtoWithNonExistingOwner.getName()).ownerId(nonExistingOwnerId).status(Project.Status.ACTIVE).build();
        when(projectMapper.toEntity(newProjectDtoWithNonExistingOwner)).thenReturn(projectEntityToSaveWithNonExistingOwner);
        when(userRepository.findById(nonExistingOwnerId)).thenReturn(Mono.empty());
        StepVerifier.create(projectService.createProject(newProjectDtoWithNonExistingOwner)).expectError(ResourceNotFoundException.class).verify();
        verify(projectMapper).toEntity(newProjectDtoWithNonExistingOwner);
        verify(userRepository).findById(nonExistingOwnerId);
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }
    // --- Тесты для updateProject ---

    @Test
    @DisplayName("updateProject - должен успешно обновить проект по существующему ID с изменением владельца")
    void updateProject_ExistingIdAndOwnerChanged_ShouldUpdateProject() {
        doAnswer(invocation -> {
            ProjectDto dto = invocation.getArgument(0);
            Project entity = invocation.getArgument(1);

            if (dto.getName() != null) {
                entity.setName(dto.getName());
            }
            if (dto.getDescription() != null) {
                entity.setDescription(dto.getDescription());
            }
            if (dto.getStatus() != null) {
                entity.setStatus(dto.getStatus());
            }
            if (dto.getOwnerId() != null) {
                entity.setOwnerId(dto.getOwnerId());
            }

            return null;
        }).when(projectMapper).updateEntityFromDto(any(ProjectDto.class), any(Project.class));

        Long newOwnerId = updateProjectDto.getOwnerId(); // 300L

        when(projectRepository.findById(existingProjectId)).thenReturn(Mono.just(existingProjectEntity));
        when(userRepository.existsById(eq(newOwnerId))).thenReturn(Mono.just(true));

        when(projectRepository.save(existingProjectEntity)).thenReturn(Mono.just(updatedProjectEntity));
        when(projectMapper.toDto(updatedProjectEntity)).thenReturn(updatedProjectDto);

        StepVerifier.create(projectService.updateProject(existingProjectId, updateProjectDto))
                .expectNext(updatedProjectDto)
                .verifyComplete();

        verify(projectRepository).findById(existingProjectId);
        verify(projectMapper).updateEntityFromDto(updateProjectDto, existingProjectEntity);
        verify(userRepository).existsById(eq(newOwnerId));
        verify(projectRepository).save(existingProjectEntity);
        verify(projectMapper).toDto(updatedProjectEntity);

        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("updateProject - должен вернуть ошибку ResourceNotFoundException, если новый владелец не найден")
    void updateProject_NewOwnerNotFound_ShouldReturnError() {
        doAnswer(invocation -> {
            ProjectDto dto = invocation.getArgument(0);
            Project entity = invocation.getArgument(1);

            if (dto.getName() != null) {
                entity.setName(dto.getName());
            }
            if (dto.getDescription() != null) {
                entity.setDescription(dto.getDescription());
            }
            if (dto.getStatus() != null) {
                entity.setStatus(dto.getStatus());
            }
            if (dto.getOwnerId() != null) {
                entity.setOwnerId(dto.getOwnerId());
            }

            return null;
        }).when(projectMapper).updateEntityFromDto(any(ProjectDto.class), any(Project.class));

        Long newOwnerId = updateProjectDto.getOwnerId(); // 300L
        when(projectRepository.findById(existingProjectId)).thenReturn(Mono.just(existingProjectEntity));
        when(userRepository.existsById(eq(newOwnerId))).thenReturn(Mono.just(false));

        StepVerifier.create(projectService.updateProject(existingProjectId, updateProjectDto))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(projectRepository).findById(existingProjectId);
        verify(projectMapper).updateEntityFromDto(updateProjectDto, existingProjectEntity);
        verify(userRepository).existsById(eq(newOwnerId));
        verify(projectRepository, never()).save(any(Project.class));
        verify(projectMapper, never()).toDto(any(Project.class));

        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("updateProject - должен успешно обновить проект по существующему ID без изменения владельца")
    void updateProject_ExistingIdAndOwnerNotChanged_ShouldUpdateProject() {
        Long oldOwnerId = existingProjectEntity.getOwnerId(); // 100L
        ProjectDto updateProjectDtoNoOwnerChange = ProjectDto.builder().name("Обновленный Проект (владелец тот же)").build();

        Project updatedProjectEntityNoOwnerChange = Project.builder()
                .id(existingProjectId).name(updateProjectDtoNoOwnerChange.getName()).description(existingProjectEntity.getDescription())
                .status(existingProjectEntity.getStatus()).ownerId(existingProjectEntity.getOwnerId()) // Владелец старый
                .createdAt(existingProjectEntity.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        ProjectDto updatedProjectDtoNoOwnerChange = ProjectDto.builder()
                .id(updatedProjectEntityNoOwnerChange.getId()).name(updatedProjectEntityNoOwnerChange.getName())
                .description(updatedProjectEntityNoOwnerChange.getDescription()).status(updatedProjectEntityNoOwnerChange.getStatus()).ownerId(updatedProjectEntityNoOwnerChange.getOwnerId())
                .createdAt(updatedProjectEntityNoOwnerChange.getCreatedAt()).updatedAt(updatedProjectEntityNoOwnerChange.getUpdatedAt())
                .build();


        when(projectRepository.findById(existingProjectId)).thenReturn(Mono.just(existingProjectEntity)); // projectEntity

        when(userRepository.existsById(oldOwnerId)).thenReturn(Mono.just(true));

        when(projectRepository.save(existingProjectEntity)).thenReturn(Mono.just(updatedProjectEntityNoOwnerChange));
        when(projectMapper.toDto(updatedProjectEntityNoOwnerChange)).thenReturn(updatedProjectDtoNoOwnerChange);

        StepVerifier.create(projectService.updateProject(existingProjectId, updateProjectDtoNoOwnerChange)).expectNext(updatedProjectDtoNoOwnerChange).verifyComplete();

        verify(projectRepository).findById(existingProjectId);
        verify(projectMapper).updateEntityFromDto(updateProjectDtoNoOwnerChange, existingProjectEntity); // Проверяем вызов маппера
        verify(userRepository).existsById(oldOwnerId);
        verify(projectRepository).save(existingProjectEntity);
        verify(projectMapper).toDto(updatedProjectEntityNoOwnerChange);

        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("updateProject - должен вернуть ошибку ResourceNotFoundException, если проект для обновления не найден")
    void updateProject_ProjectNotFound_ShouldReturnError() {
        Long nonExistingId = 999L;
        when(projectRepository.findById(nonExistingId)).thenReturn(Mono.empty());
        StepVerifier.create(projectService.updateProject(nonExistingId, updateProjectDto))
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(projectRepository).findById(nonExistingId);
        verifyNoMoreInteractions(projectMapper, userMapper, userRepository);
    }

    // --- Тесты для deleteProject ---
    @Test
    @DisplayName("deleteProject - должен успешно удалить проект по существующему ID")
    void deleteProject_ExistingId_ShouldDeleteProject() {
        when(projectRepository.findById(projectId)).thenReturn(Mono.just(projectEntity));
        when(projectRepository.deleteById(projectId)).thenReturn(Mono.empty());
        StepVerifier.create(projectService.deleteProject(projectId)).verifyComplete();
        verify(projectRepository).findById(projectId);
        verify(projectRepository).deleteById(projectId);
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }

    @Test
    @DisplayName("deleteProject - должен вернуть ошибку ResourceNotFoundException, если проект для удаления не найден")
    void deleteProject_ProjectNotFound_ShouldReturnError() {
        Long nonExistingId = 999L;
        when(projectRepository.findById(nonExistingId)).thenReturn(Mono.empty());
        StepVerifier.create(projectService.deleteProject(nonExistingId)).expectError(ResourceNotFoundException.class).verify();
        verify(projectRepository).findById(nonExistingId);
        verify(projectRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(projectRepository, projectMapper, userMapper, userRepository);
    }
}