package com.phil.members.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.StringJoiner;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@SequenceGenerator(name="seq", sequenceName="seq", initialValue=7, allocationSize=10)
@XmlRootElement
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Member extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="seq")
    private Long id;

    private String name;

    @Size(min = 1, max = 25)
    @Pattern(regexp = "[^0-9]*", message = "Must not contain numbers")
    @Transient
    private String firstName;

    @Size(min = 1, max = 25)
    @Pattern(regexp = "[^0-9]*", message = "Must not contain numbers")
    @Transient
    private String lastName;

    @Transient
    private StringJoiner listNames = new StringJoiner(" ");
    
    @NotNull
    @NotEmpty
    @Email
    private String email;

    @NotNull
    @Size(min = 10, max = 12)
    @Digits(fraction = 0, integer = 12)
    @Column(name = "phone_number")
    private String phoneNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        
        String[] names = name.split("\\s+");
		firstName = names[0];
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateName(firstName);
		
		name = listNames.toString();
    }

    public String getLastName() {

        String[] names = name.split("\\s");
		
		lastName = names[1];
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateName(lastName);
		name = listNames.toString();
    }
    private void updateName(String name) {
		
		
		listNames.add(name);
		
	}
}
