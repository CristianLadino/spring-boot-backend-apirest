package com.bolsadeideas.spring.boot.backend.apirest.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import com.bolsadeideas.spring.boot.backend.apirest.models.entity.Client;

public interface IClientDao extends JpaRepository<Client, Long> {

}
