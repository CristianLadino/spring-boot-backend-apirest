package com.bolsadeideas.spring.boot.backend.apirest.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bolsadeideas.spring.boot.backend.apirest.models.entity.Client;
import com.bolsadeideas.spring.boot.backend.apirest.models.services.IClientService;

@CrossOrigin(origins = { "http://localhost:4200" })
@RestController
@RequestMapping("/api")
public class ClientRestController {

	@Autowired
	private IClientService clientService;
	
	private final Logger log = LoggerFactory.getLogger( ClientRestController.class) ;

	@GetMapping("/clients")
	public List<Client> index() {
		return clientService.findAll();
	}

	@GetMapping("/clients/page/{page}")
	public Page<Client> index(@PathVariable Integer page) {
		Pageable pageable = PageRequest.of(page, 3);
		return clientService.findAll(pageable);
	}

	@GetMapping("/clients/{id}")
	public ResponseEntity<?> show(@PathVariable Long id) {

		Client client = null;
		Map<String, Object> response = new HashMap<>();

		try {
			client = clientService.findById(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error when querying the database");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (client == null) {
			response.put("mensaje", "The client with ID:".concat(id.toString().concat(" does not exist")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Client>(client, HttpStatus.OK);
	}

	@PostMapping("/clients")
	public ResponseEntity<?> create(@Valid @RequestBody Client client, BindingResult result) {

		Client newClient = null;
		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			newClient = clientService.save(client);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error when inserting into database");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("mensaje", "The client was created successfully");
		response.put("client", newClient);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@PutMapping("/clients/{id}")
	public ResponseEntity<?> update(@Valid @RequestBody Client client, BindingResult result, @PathVariable Long id) {
		Client currentClient = clientService.findById(id);
		Client updateClient = null;
		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		if (currentClient == null) {
			response.put("mensaje", "Error: the client with ID:"
					.concat(id.toString().concat(" could not update because it does not exist")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		try {

			currentClient.setName(client.getName());
			currentClient.setLast_name(client.getLast_name());
			currentClient.setEmail(client.getEmail());
			currentClient.setCreateAt(client.getCreateAt());

			updateClient = clientService.save(currentClient);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error when update into database");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "The client was update successfully");
		response.put("client", updateClient);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@DeleteMapping("/clients/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		Map<String, Object> response = new HashMap<>();

		try {
			
			Client client = clientService.findById(id);
			String nameOldPhoto = client.getPhoto();

			if (nameOldPhoto != null && nameOldPhoto.length() > 0) {
				Path rootOldPhoto = Paths.get("uploads").resolve(nameOldPhoto).toAbsolutePath();
				File fileOldPhoto = rootOldPhoto.toFile();

				if (fileOldPhoto.exists() && fileOldPhoto.canRead()) {
					fileOldPhoto.delete();
				}
			}

			clientService.delete(id);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error when delete into database");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "The client was delete successfully");
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}

	@PostMapping("/clients/upload")
	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("id") Long id) {
		Map<String, Object> response = new HashMap<>();

		Client client = clientService.findById(id);

		if (!file.isEmpty()) {

			String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "");
			Path fileRoot = Paths.get("uploads").resolve(fileName).toAbsolutePath();
			
			log.info(fileRoot.toString());

			try {
				Files.copy(file.getInputStream(), fileRoot);
			} catch (IOException e) {
				response.put("mensaje", "Error when uploading the photo: " + fileName);
				response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String nameOldPhoto = client.getPhoto();

			if (nameOldPhoto != null && nameOldPhoto.length() > 0) {
				Path rootOldPhoto = Paths.get("uploads").resolve(nameOldPhoto).toAbsolutePath();
				File fileOldPhoto = rootOldPhoto.toFile();

				if (fileOldPhoto.exists() && fileOldPhoto.canRead()) {
					fileOldPhoto.delete();
				}
			}

			client.setPhoto(fileName);

			clientService.save(client);

			response.put("client", client);
			response.put("message", "The photo was uploaded successfully " + fileName);
		}

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@GetMapping("/uploads/img/{namePhoto:.+}")
	public ResponseEntity<Resource> viewPhoto(@PathVariable String namePhoto) {
		
		Path fileRoot = Paths.get("uploads").resolve(namePhoto).toAbsolutePath();
		Resource resource = null;
		
		log.info(fileRoot.toString());
		
		try {
			resource = new UrlResource(fileRoot.toUri());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if(!resource.exists() && !resource.isReadable()) {
			throw new RuntimeException("Error image could not be loaded " + namePhoto);
		}
		
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
		
		return new ResponseEntity<Resource>(resource, header , HttpStatus.OK);
	}
}
