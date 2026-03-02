package com.semi.simlogistics.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.ModelUploadResultDTO;
import com.semi.simlogistics.web.dto.ModelSummaryDTO;
import com.semi.simlogistics.web.dto.PagedResultDTO;
import com.semi.simlogistics.web.exception.GlobalExceptionHandler;
import com.semi.simlogistics.web.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ModelController filter contract.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

    @Mock
    private ModelService modelService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ModelController controller = new ModelController(modelService, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldForwardTypeStatusAndSearchFiltersToService() throws Exception {
        ModelSummaryDTO dto = new ModelSummaryDTO();
        dto.setModelId("model-1");
        dto.setName("OHT Model");
        dto.setModelType("OHT_VEHICLE");
        dto.setStatus("ACTIVE");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());

        PagedResultDTO<ModelSummaryDTO> result = new PagedResultDTO<>(List.of(dto), 1, 0, 20);
        when(modelService.getModels(0, 20, "OHT_VEHICLE", "ACTIVE", "oht")).thenReturn(result);

        mockMvc.perform(get("/api/v1/models")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("type", "OHT_VEHICLE")
                        .param("status", "ACTIVE")
                        .param("search", "oht"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].modelId").value("model-1"));

        verify(modelService).getModels(0, 20, "OHT_VEHICLE", "ACTIVE", "oht");
    }

    @Test
    void shouldReturnFileTooLargeWhenUploadExceedsLimit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.glb",
                "model/gltf-binary",
                "fake-binary-content".getBytes()
        );

        doThrow(new MaxUploadSizeExceededException(1048576L))
                .when(modelService)
                .uploadModel(any(), eq("large-model"), eq("MACHINE"), any(), eq("1.0.0"), any(), any());

        mockMvc.perform(multipart("/api/v1/models/upload")
                        .file(file)
                        .param("name", "large-model")
                        .param("modelType", "MACHINE")
                        .param("version", "1.0.0"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestWhenTransformScaleArrayLengthIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "model/gltf-binary",
                "fake-binary-content".getBytes()
        );

        String metadata = """
                {
                  "type": "MACHINE",
                  "version": "1.0.0",
                  "transform": {
                    "scale": [1, 1],
                    "rotation": { "x": 0, "y": 0, "z": 0 },
                    "pivot": { "x": 0, "y": 0, "z": 0 }
                  }
                }
                """;

        mockMvc.perform(multipart("/api/v1/models/upload")
                        .file(file)
                        .param("name", "invalid-transform-model")
                        .param("modelType", "MACHINE")
                        .param("version", "1.0.0")
                        .param("metadata", metadata))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("metadata/transform格式错误"));

        verify(modelService, never()).uploadModel(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateModelWhenTransformUsesObjectFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "model/gltf-binary",
                "fake-binary-content".getBytes()
        );

        ModelUploadResultDTO uploadResultDTO = new ModelUploadResultDTO();
        uploadResultDTO.setModelId("model-obj");
        uploadResultDTO.setVersionId("version-obj");
        uploadResultDTO.setVersion("1.0.0");
        uploadResultDTO.setFileUrl("/objects/model.glb");

        when(modelService.uploadModel(any(), eq("object-transform-model"), eq("MACHINE"), any(), eq("1.0.0"), any(), any()))
                .thenReturn(uploadResultDTO);

        String metadata = """
                {
                  "type": "MACHINE",
                  "version": "1.0.0",
                  "transform": {
                    "scale": { "x": 1, "y": 1, "z": 1 },
                    "rotation": { "x": 0, "y": 0, "z": 0 },
                    "pivot": { "x": 0, "y": 0, "z": 0 }
                  }
                }
                """;

        mockMvc.perform(multipart("/api/v1/models/upload")
                        .file(file)
                        .param("name", "object-transform-model")
                        .param("modelType", "MACHINE")
                        .param("version", "1.0.0")
                        .param("metadata", metadata))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.modelId").value("model-obj"));

        verify(modelService).uploadModel(
                any(),
                eq("object-transform-model"),
                eq("MACHINE"),
                any(),
                eq("1.0.0"),
                any(),
                argThat(transform -> transform != null
                        && transform.getScale() != null
                        && Double.compare(transform.getScale().getX(), 1.0D) == 0
                        && Double.compare(transform.getScale().getY(), 1.0D) == 0
                        && Double.compare(transform.getScale().getZ(), 1.0D) == 0)
        );
    }

    @Test
    void shouldCreateModelWhenTransformScaleUsesLegacyArrayFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "model/gltf-binary",
                "fake-binary-content".getBytes()
        );

        ModelUploadResultDTO uploadResultDTO = new ModelUploadResultDTO();
        uploadResultDTO.setModelId("model-array");
        uploadResultDTO.setVersionId("version-array");
        uploadResultDTO.setVersion("1.0.0");
        uploadResultDTO.setFileUrl("/objects/model-array.glb");

        when(modelService.uploadModel(any(), eq("array-transform-model"), eq("MACHINE"), any(), eq("1.0.0"), any(), any()))
                .thenReturn(uploadResultDTO);

        String metadata = """
                {
                  "type": "MACHINE",
                  "version": "1.0.0",
                  "transform": {
                    "scale": [1.5, 2.0, 2.5],
                    "rotation": { "x": 0, "y": 0, "z": 0 },
                    "pivot": { "x": 0, "y": 0, "z": 0 }
                  }
                }
                """;

        mockMvc.perform(multipart("/api/v1/models/upload")
                        .file(file)
                        .param("name", "array-transform-model")
                        .param("modelType", "MACHINE")
                        .param("version", "1.0.0")
                        .param("metadata", metadata))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.modelId").value("model-array"));

        verify(modelService).uploadModel(
                any(),
                eq("array-transform-model"),
                eq("MACHINE"),
                any(),
                eq("1.0.0"),
                any(),
                argThat(transform -> transform != null
                        && transform.getScale() != null
                        && Double.compare(transform.getScale().getX(), 1.5D) == 0
                        && Double.compare(transform.getScale().getY(), 2.0D) == 0
                        && Double.compare(transform.getScale().getZ(), 2.5D) == 0)
        );
    }
}
