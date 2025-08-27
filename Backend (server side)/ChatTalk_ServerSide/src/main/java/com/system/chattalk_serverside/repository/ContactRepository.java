package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact,Long>{
}
