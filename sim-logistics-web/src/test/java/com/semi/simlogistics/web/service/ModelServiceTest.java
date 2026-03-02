package com.semi.simlogistics.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.control.port.storage.ObjectMetadata;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.domain.model.EntityModelBindingRepository;
import com.semi.simlogistics.web.domain.model.Model;
import com.semi.simlogistics.web.domain.model.ModelRepository;
import com.semi.simlogistics.web.domain.model.ModelVersion;
import com.semi.simlogistics.web.domain.model.ModelVersionRepository;
import com.semi.simlogistics.web.dto.ModelSummaryDTO;
import com.semi.simlogistics.web.dto.PagedResultDTO;
import com.semi.simlogistics.web.dto.SetEntityModelBindingRequestDTO;
import com.semi.simlogistics.web.dto.ModelUploadResultDTO;
import com.semi.simlogistics.web.exception.ResourceNotFoundException;
import com.semi.simlogistics.web.exception.ValidationException;
import com.semi.simlogistics.web.infrastructure.storage.ObjectStorageCompensationPort;
import com.semi.simlogistics.web.infrastructure.persistence.model.File;
import com.semi.simlogistics.web.infrastructure.persistence.model.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for model upload persistence chain.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelVersionRepository modelVersionRepository;

    @Mock
    private EntityModelBindingRepository entityModelBindingRepository;

    @Mock
    private FileRepository fileRepository;

    private ObjectStoragePort objectStoragePort;

    private ModelService modelService;

    @BeforeEach
    void setUp() {
        objectStoragePort = mock(ObjectStoragePort.class, withSettings().extraInterfaces(ObjectStorageCompensationPort.class));
        modelService = new ModelService(
                modelRepository,
                modelVersionRepository,
                entityModelBindingRepository,
                fileRepository,
                objectStoragePort,
                new ObjectMapper(),
                "sim-artifacts"
        );
    }

    @Test
    void uploadModel_shouldInsertModelThenVersion_withValidForeignKey() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oht.glb",
                "model/gltf-binary",
                "glb-binary-data".getBytes()
        );

        when(modelRepository.existsByName("OHT Crane")).thenReturn(false);
        when(objectStoragePort.putObject(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn(new ObjectMetadata(
                        "sim-artifacts",
                        "models/fake/key",
                        file.getSize(),
                        "model/gltf-binary",
                        "etag-1",
                        Instant.now()
                ));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelVersionRepository.save(any(ModelVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ModelUploadResultDTO result = modelService.uploadModel(
                file,
                "OHT Crane",
                "OHT_VEHICLE",
                "upload-test",
                "1.0.0",
                null,
                null
        );

        // Then
        assertThat(result.getModelId()).isNotBlank();
        assertThat(result.getVersionId()).isNotBlank();
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getFileUrl()).contains("/sim-artifacts/models/");

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<ModelVersion> versionCaptor = ArgumentCaptor.forClass(ModelVersion.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);

        verify(fileRepository).save(fileCaptor.capture());
        verify(modelVersionRepository).save(versionCaptor.capture());
        verify(modelRepository, atLeastOnce()).save(modelCaptor.capture());
        verify(objectStoragePort).putObject(eq("sim-artifacts"), contains("models/"), any(), eq(file.getSize()), eq("model/gltf-binary"));
        InOrder saveOrder = inOrder(modelRepository, modelVersionRepository);
        saveOrder.verify(modelRepository).save(any(Model.class));
        saveOrder.verify(modelVersionRepository).save(any(ModelVersion.class));

        File savedFile = fileCaptor.getValue();
        ModelVersion savedVersion = versionCaptor.getValue();
        Model savedModel = modelCaptor.getValue();

        assertThat(savedFile.getStorageBucket()).isEqualTo("sim-artifacts");
        assertThat(savedFile.getStorageKey()).startsWith("models/" + savedModel.getModelId() + "/");
        assertThat(savedVersion.getModelId()).isEqualTo(savedModel.getId());
        assertThat(savedVersion.getFileId()).isEqualTo(savedFile.getId());
        assertThat(savedModel.getDefaultVersionId()).isEqualTo(savedVersion.getVersionId());
    }

    @Test
    void uploadModel_shouldUseModelLibraryIdAsVersionModelId() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oht.glb",
                "model/gltf-binary",
                "glb-binary-data".getBytes()
        );

        when(modelRepository.existsByName("OHT Crane")).thenReturn(false);
        when(objectStoragePort.putObject(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn(new ObjectMetadata(
                        "sim-artifacts",
                        "models/fake/key",
                        file.getSize(),
                        "model/gltf-binary",
                        "etag-1",
                        Instant.now()
                ));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelVersionRepository.save(any(ModelVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        modelService.uploadModel(
                file,
                "OHT Crane",
                "OHT_VEHICLE",
                "upload-test",
                "1.0.0",
                null,
                null
        );

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        ArgumentCaptor<ModelVersion> versionCaptor = ArgumentCaptor.forClass(ModelVersion.class);
        verify(modelRepository, atLeastOnce()).save(modelCaptor.capture());
        verify(modelVersionRepository).save(versionCaptor.capture());
        String modelLibraryId = modelCaptor.getAllValues().get(0).getId();
        assertThat(modelLibraryId).isNotBlank();
        assertThat(versionCaptor.getValue().getModelId()).isEqualTo(modelLibraryId);
    }

    @Test
    void uploadModel_shouldRollbackAndDeleteObjectWhenVersionInsertFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oht.glb",
                "model/gltf-binary",
                "glb-binary-data".getBytes()
        );

        when(modelRepository.existsByName("OHT Crane")).thenReturn(false);
        when(objectStoragePort.putObject(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn(new ObjectMetadata(
                        "sim-artifacts",
                        "models/fake/key",
                        file.getSize(),
                        "model/gltf-binary",
                        "etag-1",
                        Instant.now()
                ));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelVersionRepository.save(any(ModelVersion.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("fk_model_versions_model"));

        assertThatThrownBy(() -> modelService.uploadModel(
                file,
                "OHT Crane",
                "OHT_VEHICLE",
                "upload-test",
                "1.0.0",
                null,
                null
        )).isInstanceOf(RuntimeException.class);

        ObjectStorageCompensationPort compensationPort = (ObjectStorageCompensationPort) objectStoragePort;
        verify(compensationPort).deleteObject(eq("sim-artifacts"), contains("models/"));
    }

    @Test
    void deleteModel_shouldDeleteVersionsBeforeFiles() {
        String modelId = "model-db-1";
        String versionId = "version-1";
        String fileId = "file-1";

        Model model = new Model();
        model.setId(modelId);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelVersion version = new ModelVersion();
        version.setVersionId(versionId);
        version.setFileId(fileId);
        when(modelVersionRepository.findByModelId(modelId)).thenReturn(java.util.List.of(version));

        File file = new File();
        file.setId(fileId);
        file.setStorageBucket("sim-artifacts");
        file.setStorageKey("models/MODEL-1/oht.glb");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(modelVersionRepository.countByFileId(fileId)).thenReturn(0L);

        modelService.deleteModel(modelId);

        InOrder deleteOrder = inOrder(modelVersionRepository, fileRepository);
        deleteOrder.verify(modelVersionRepository).deleteById(versionId);
        deleteOrder.verify(fileRepository).deleteById(fileId);
        verify(modelRepository).deleteById(modelId);
    }

    @Test
    void deleteModel_shouldClearDefaultVersionBeforeDeletingVersions() {
        String modelId = "model-db-1";
        String versionId = "version-1";
        String fileId = "file-1";

        Model model = new Model();
        model.setId(modelId);
        model.setDefaultVersionId(versionId);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelVersion version = new ModelVersion();
        version.setVersionId(versionId);
        version.setFileId(fileId);
        when(modelVersionRepository.findByModelId(modelId)).thenReturn(java.util.List.of(version));

        File file = new File();
        file.setId(fileId);
        file.setStorageBucket("sim-artifacts");
        file.setStorageKey("models/MODEL-1/oht.glb");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(modelVersionRepository.countByFileId(fileId)).thenReturn(0L);

        modelService.deleteModel(modelId);

        InOrder deleteOrder = inOrder(modelRepository, modelVersionRepository);
        deleteOrder.verify(modelRepository).clearDefaultVersionId(modelId);
        deleteOrder.verify(modelVersionRepository).deleteById(versionId);
        verify(modelRepository).deleteById(modelId);
    }

    @Test
    void deleteModel_shouldClearVersionReferencesBeforeDeletingEachVersion() {
        String modelId = "model-db-1";
        String versionId = "version-1";
        String fileId = "file-1";

        Model model = new Model();
        model.setId(modelId);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelVersion version = new ModelVersion();
        version.setVersionId(versionId);
        version.setFileId(fileId);
        when(modelVersionRepository.findByModelId(modelId)).thenReturn(java.util.List.of(version));

        File file = new File();
        file.setId(fileId);
        file.setStorageBucket("sim-artifacts");
        file.setStorageKey("models/MODEL-1/oht.glb");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(modelVersionRepository.countByFileId(fileId)).thenReturn(0L);

        modelService.deleteModel(modelId);

        InOrder deleteOrder = inOrder(modelRepository, modelVersionRepository);
        deleteOrder.verify(modelRepository).clearDefaultVersionReferences(versionId);
        deleteOrder.verify(modelVersionRepository).deleteById(versionId);
        verify(modelRepository).deleteById(modelId);
    }

    @Test
    void deleteModel_shouldNotDeleteFileWhenStillReferenced() {
        String modelId = "model-db-1";
        String versionId = "version-1";
        String fileId = "file-1";

        Model model = new Model();
        model.setId(modelId);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelVersion version = new ModelVersion();
        version.setVersionId(versionId);
        version.setFileId(fileId);
        when(modelVersionRepository.findByModelId(modelId)).thenReturn(java.util.List.of(version));

        File file = new File();
        file.setId(fileId);
        file.setStorageBucket("sim-artifacts");
        file.setStorageKey("models/MODEL-1/oht.glb");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(modelVersionRepository.countByFileId(fileId)).thenReturn(1L);

        modelService.deleteModel(modelId);

        verify(modelVersionRepository).deleteById(versionId);
        verify(modelRepository).deleteById(modelId);
        verify(fileRepository, never()).deleteById(fileId);
        ObjectStorageCompensationPort compensationPort = (ObjectStorageCompensationPort) objectStoragePort;
        verify(compensationPort, never()).deleteObject(anyString(), anyString());
    }

    @Test
    void deleteModel_shouldReturnSuccessWhenMinioDeleteFailsButDbDeleted() {
        String modelId = "model-db-1";
        String versionId = "version-1";
        String fileId = "file-1";

        Model model = new Model();
        model.setId(modelId);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelVersion version = new ModelVersion();
        version.setVersionId(versionId);
        version.setFileId(fileId);
        when(modelVersionRepository.findByModelId(modelId)).thenReturn(java.util.List.of(version));

        File file = new File();
        file.setId(fileId);
        file.setStorageBucket("sim-artifacts");
        file.setStorageKey("models/MODEL-1/oht.glb");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(modelVersionRepository.countByFileId(fileId)).thenReturn(0L);

        ObjectStorageCompensationPort compensationPort = (ObjectStorageCompensationPort) objectStoragePort;
        doThrow(new RuntimeException("minio delete failed"))
                .when(compensationPort).deleteObject("sim-artifacts", "models/MODEL-1/oht.glb");

        assertThatCode(() -> modelService.deleteModel(modelId)).doesNotThrowAnyException();

        verify(modelVersionRepository).deleteById(versionId);
        verify(modelRepository).deleteById(modelId);
        verify(fileRepository).deleteById(fileId);
    }

    @Test
    void shouldReturnNotFoundWhenVersionIdDoesNotExistInSetBinding() {
        // Given
        SetEntityModelBindingRequestDTO request = new SetEntityModelBindingRequestDTO();
        request.setVersionId("missing-version");
        when(modelVersionRepository.findById("missing-version")).thenReturn(java.util.Optional.empty());

        // When/Then
        assertThatThrownBy(() -> modelService.setEntityBinding("scene-1", "entity-1", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectWhenModelIdAndVersionIdMismatchInSetBinding() {
        // Given
        SetEntityModelBindingRequestDTO request = new SetEntityModelBindingRequestDTO();
        request.setModelId("model-A");
        request.setVersionId("version-1");
        ModelVersion version = new ModelVersion();
        version.setVersionId("version-1");
        version.setModelId("model-B");
        when(modelVersionRepository.findById("version-1")).thenReturn(java.util.Optional.of(version));

        // When/Then
        assertThatThrownBy(() -> modelService.setEntityBinding("scene-1", "entity-1", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("modelId");
    }

    @Test
    void shouldApplyTypeStatusSearchFiltersWhenListingModels() {
        // Given
        when(modelRepository.findByFilters(0, 20, "OHT_VEHICLE", "ACTIVE", "oht"))
                .thenReturn(java.util.List.of(new ModelSummaryDTO()));
        when(modelRepository.countByFilters("OHT_VEHICLE", "ACTIVE", "oht"))
                .thenReturn(1L);

        // When
        PagedResultDTO<ModelSummaryDTO> result =
                modelService.getModels(0, 20, "OHT_VEHICLE", "ACTIVE", "oht");

        // Then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        verify(modelRepository).findByFilters(0, 20, "OHT_VEHICLE", "ACTIVE", "oht");
        verify(modelRepository).countByFilters("OHT_VEHICLE", "ACTIVE", "oht");
    }
}
