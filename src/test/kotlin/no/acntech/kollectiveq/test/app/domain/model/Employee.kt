package no.acntech.kollectiveq.test.app.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "employees")
data class Employee(

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   var id: Long? = null,

   @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
   @CreationTimestamp
   val createdAt: Instant = Instant.now(),

   @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
   @UpdateTimestamp
   val updatedAt: Instant = Instant.now(),

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "department_id", nullable = false)
   var department: Department? = null,

   @Column(name = "first_name", nullable = false)
   var firstName: String? = null,

   @Column(name = "last_name", nullable = false)
   var lastName: String? = null,

   @Column(name = "year_of_birth", nullable = false)
   var yearOfBirth: Int? = null,

   @Column(name = "address_line_1", nullable = false)
   var addressLine1: String? = null,

   @Column(name = "address_line_2")
   var addressLine2: String? = null,

   @Column(name = "postal_code", nullable = false)
   var postalCode: String? = null,

   @Column(name = "postal_area", nullable = false)
   var postalArea: String? = null,

   @Column(name = "country", nullable = false)
   var country: String? = null,

   @Column(name = "birth_date", nullable = false, columnDefinition = "DATE")
   @Temporal(TemporalType.DATE)
   var birthDate: LocalDate? = null,

   @Column(name = "last_login")
   @Temporal(TemporalType.TIMESTAMP)
   var lastLogin: Instant? = null,

   @Column(name = "last_logout")
   var lastLogout: Instant? = null,

   @Column(name = "password")
   var password: String? = null,

   @Column(name = "is_part_time")
   var isPartTime: Boolean = false,

   @ManyToMany(mappedBy = "members", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
   val projects: Set<Project> = HashSet(),

   ) : Serializable {

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class != other::class) return false

      other as Employee

      if (id != other.id) return false

      return true
   }

   override fun hashCode(): Int {
      return id?.hashCode() ?: 0
   }

   override fun toString(): String {
      return "Employee(id=$id, firstName=$firstName lastName=$lastName)"
   }
}
