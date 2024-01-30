package com.phil.members.service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import com.phil.members.model.Member;

import io.quarkus.panache.common.Sort;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
@Transactional(SUPPORTS)
public class MemberRegistration {

    public Member findById(Long id) {
        return Member.findById(id);
    }

    public Member findByEmail(String email) {
        return Member.find("email", email).firstResult();
    }

    public List<Member> findAllOrderedByName() {
        return Member.listAll(Sort.by("name").ascending());
     }

     @Transactional(REQUIRED)
    public Member register(@Valid Member member) {

        Member newMember = new Member();
        newMember.setEmail(member.getEmail());
        newMember.setFirstName(member.getFirstName());
        newMember.setLastName(member.getLastName());
        newMember.setPhoneNumber(member.getPhoneNumber());
        Member.persist(newMember);
        
        return Member.find("email", newMember.getEmail()).firstResult();
    }
    
    @Transactional(REQUIRED)
    public void unregister(Member member) {
        Member.delete("email", member.getEmail());
        
    }
}
