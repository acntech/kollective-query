package no.acntech.kollectiveq.test.app.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "departments")
data class Department(

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   var id: Long? = null,

   @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
   @CreationTimestamp
   val createdAt: Instant = Instant.now(),

   @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
   @UpdateTimestamp
   val updatedAt: Instant = Instant.now(),

   @Column(name = "name", nullable = false)
   var name: String? = null,

   @OneToMany(mappedBy = "department", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
   val employees: Set<Employee> = HashSet(),

   ) : Serializable {

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class != other::class) return false

      other as Department

      if (id != other.id) return false

      return true
   }

   override fun hashCode(): Int {
      return id?.hashCode() ?: 0
   }

   override fun toString(): String {
      return "Department(id=$id, name=$name)"
   }
}