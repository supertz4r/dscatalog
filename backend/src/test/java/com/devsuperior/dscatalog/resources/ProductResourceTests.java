package com.devsuperior.dscatalog.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.devsuperior.dscatalog.dto.ProductDTO;
import com.devsuperior.dscatalog.services.ProductService;
import com.devsuperior.dscatalog.services.exceptions.DatabaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dscatalog.tests.Factory;
import com.devsuperior.dscatalog.tests.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductResourceTests {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean
	private ProductService service;
	
	@Autowired
	private TokenUtil tokenUtil;
	
	private long existingId;
	private long nonExistingId;
	private long dependentId;
	
	private String username;
	private String password;
	
	private ProductDTO productDTO;
	private PageImpl<ProductDTO> page;
	
	@BeforeEach
	void setUp() throws Exception{
		
		username = "maria@gmail.com";
		password = "123456";
		
		existingId = 1L;
		nonExistingId = 2L;
		dependentId = 3L;
		productDTO = Factory.createProductDTO();
		page = new PageImpl<>(List.of(productDTO));
		
		when(service.findAllPaged(any(), any(), any())).thenReturn(page);
		
		when(service.findById(existingId)).thenReturn(productDTO);
		when(service.findById(nonExistingId)).thenThrow(ResourceNotFoundException.class);
		
		when(service.update(eq(existingId), any())).thenReturn(productDTO);
		when(service.update(eq(nonExistingId), any())).thenThrow(ResourceNotFoundException.class);
		
		doNothing().when(service).delete(existingId);
		doThrow(ResourceNotFoundException.class).when(service).delete(nonExistingId);
		doThrow(DatabaseException.class).when(service).delete(dependentId);
		
		when(service.insert(any())).thenReturn(productDTO);
	}
	
	@Test
	public void findAllShouldReturnPage() throws Exception {
		mockMvc.perform(get("/products")).andExpect(status().isOk());
	}
	
	@Test
	public void findByIdShouldReturnProductWhenIdExists() throws Exception {
		mockMvc.perform(get("/products/{id}", existingId)).andExpect(status().isOk())
		       .andExpect(jsonPath("$.id").exists())
		       .andExpect(jsonPath("$.name").exists())
		       .andExpect(jsonPath("$.description").exists());
	}
	
	@Test
	public void findByIdShouldReturnNotFoundWhenIdDoesNotExists() throws Exception {
		mockMvc.perform(get("/products/{id}", nonExistingId)).andExpect(status().isNotFound());
	}
	
	@Test
	public void updateShouldReturnProductDTOWhenIdExists() throws Exception {
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
		
		String jsonBody = objectMapper.writeValueAsString(productDTO);
		
		mockMvc.perform(put("/products/{id}", existingId)
			   .header("Authorization", "Bearer " + accessToken)
			   .content(jsonBody).contentType(MediaType.APPLICATION_JSON)
			   .accept(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.id").exists())
		       .andExpect(jsonPath("$.name").exists())
		       .andExpect(jsonPath("$.description").exists());
	}
	
	@Test
    public void updateShouldReturnNotFoundWhenIdDoesNotExists() throws Exception {
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
    	
    	String jsonBody = objectMapper.writeValueAsString(productDTO);
    	
    	mockMvc.perform(put("/products/{id}", nonExistingId)
    		   .header("Authorization", "Bearer " + accessToken)
 			   .content(jsonBody).contentType(MediaType.APPLICATION_JSON)
 			   .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}
	
	@Test
	public void insertShouldReturnCreatedAndProduct() throws Exception{
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
		
		String jsonBody = objectMapper.writeValueAsString(productDTO);
		
		mockMvc.perform(post("/products").content(jsonBody)
			   .header("Authorization", "Bearer " + accessToken)
			   .contentType(MediaType.APPLICATION_JSON)
	 		   .accept(MediaType.APPLICATION_JSON))
			   .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.id").exists())
		       .andExpect(jsonPath("$.name").exists())
		       .andExpect(jsonPath("$.description").exists());
	}
	
	@Test
	public void deleteShouldReturnNoContentWhenIdExists() throws Exception{
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
		
		mockMvc.perform(delete("/products/{id}", existingId)
		       .header("Authorization", "Bearer " + accessToken))       
		       .andExpect(status().isNoContent());
	}
	
	@Test
	public void deleteShouldReturnNotFoundWhenIdDoesNotExists() throws Exception{
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
		
		mockMvc.perform(delete("/products/{id}", nonExistingId)
		       .header("Authorization", "Bearer " + accessToken))
		       .andExpect(status().isNotFound());
	}
}
