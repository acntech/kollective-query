package no.acntech.kollectiveq.test.app.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "projects")
data class Project(

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
   @JoinColumn(name = "leader_id", nullable = false)
   var leader: Employee? = null,

   @Column(name = "name", nullable = false)
   var name: String = "",

   @Column(name = "status", nullable = false)
   var status: String = "ACTIVE",

   @Column(name = "start_date", nullable = false, columnDefinition = "DATE")
   var startDate: LocalDate = LocalDate.now(),

   @Column(name = "end_date")
   var endDate: LocalDate? = null,

   @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
   @JoinTable(
      name = "project_members",
      joinColumns = [JoinColumn(name = "project_id", referencedColumnName = "id")],
      inverseJoinColumns = [JoinColumn(name = "employee_id", referencedColumnName = "id")]
   )
   val members: Set<Employee> = HashSet(),

   ) : Serializable {

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class != other::class) return false

      other as Project

      return id == other.id
   }

   override fun hashCode(): Int {
      return id?.hashCode() ?: 0
   }

   override fun toString(): String {
      return "Project(id=$id, name=$name)"
   }

}
