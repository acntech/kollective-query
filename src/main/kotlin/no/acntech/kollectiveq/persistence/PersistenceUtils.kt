package no.acntech.kollectiveq.persistence

import jakarta.persistence.*
import jakarta.persistence.metamodel.*
import no.acntech.kollectiveq.text.DOT
import no.acntech.kollectiveq.text.PERIOD
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.jvm.Transient
import kotlin.reflect.KClass


/**
 * This utility class provides methods for working with persistence-related operations,
 * such as retrieving entity fields, finding entity types, determining attribute types,
 * and performing other common tasks related to persistence.
 */
object PersistenceUtils {

   private val PRIMITIVE_NUMBER_CLASSES: Set<Class<out Any>?> = setOf(
      Byte::class.javaPrimitiveType,
      Short::class.javaPrimitiveType,
      Int::class.javaPrimitiveType,
      Long::class.javaPrimitiveType,
      Float::class.javaPrimitiveType,
      Double::class.javaPrimitiveType
   )

   private val KOTLIN_NUMBER_TYPES: Set<KClass<out Number>> = setOf(
      Byte::class,
      Short::class,
      Int::class,
      Long::class,
      Float::class,
      Double::class,
      // Nullable types are represented by the same KClass in Kotlin,
      // so there's no need to explicitly list them separately
   )

   /**
    * Retrieves the set of field names for a given entity class from the metamodel.
    *
    * @param metamodel the metamodel instance to use for entity analysis
    * @param entityClass the class representing the entity
    * @return a set of field names for the entity class
    * @throws ClassNotFoundException if the entity class is not found in the classpath
    */
   @Throws(ClassNotFoundException::class)
   fun getEntityFields(metamodel: Metamodel, entityClass: Class<*>): Set<String> {
      val fieldNames = mutableSetOf<String>()

      // Check if the class is an entity
      if (entityClass.isAnnotationPresent(Entity::class.java)) {
         val entityType = metamodel.entity(entityClass)

         // Get the attributes of the entity and filter out transient ones
         entityType.attributes.forEach { attribute ->
            try {
               val field = entityClass.getDeclaredField(attribute.name)
               if (!field.isAnnotationPresent(Transient::class.java)) {
                  fieldNames.add(attribute.name)
               }
            } catch (ignore: NoSuchFieldException) {
            }
         }
      }
      return fieldNames
   }

   /**
    * Retrieves the entity type with the given name from the provided metamodel.
    *
    * @param metamodel The metamodel containing the entities.
    * @param entityName The name of the entity type to retrieve.
    * @return The EntityType representing the entity type with the given name,
    *         or null if no entity type with the given name is found.
    */
   fun getEntityType(metamodel: Metamodel, entityName: String): EntityType<*>? {
      return metamodel.entities.find { it.name == entityName }
   }

   /**
    * Retrieves the target entity type for a plural attribute.
    *
    * @param metamodel The metamodel containing entity type information.
    * @param attribute The attribute for which to retrieve the target entity type.
    * @return The target entity type of the attribute.
    * @throws IllegalArgumentException if the attribute is not a collection type or if the entity type cannot be determined.
    */
   fun getTargetEntityTypeForPluralAttribute(metamodel: Metamodel, attribute: Attribute<*, *>): EntityType<*> {

      // Ensure that the attribute is a PluralAttribute (Collection, List, Set, etc.)
      if (attribute !is PluralAttribute<*, *, *>) {
         throw IllegalArgumentException("Attribute ${attribute.name} is not a collection type.")
      }

      // Retrieve the bound type, which is the type of the elements contained within the collection
      val boundType = attribute.elementType

      // If the bound type is an EntityType, return it directly
      return if (boundType is EntityType<*>) {
         boundType
      } else {
         // Otherwise, try to retrieve the EntityType using the bound type's javaType
         val javaType = boundType.javaType
         if (javaType is Class<*>) {
            // Retrieve the EntityType from the Metamodel using the javaType of the bound type
            metamodel.entity(javaType)
         } else {
            throw IllegalArgumentException("Could not determine the entity type for attribute '${attribute.name}'")
         }
      }
   }

   /**
    * Checks if the given attribute is a number type.
    *
    * @param attribute The attribute to check.
    * @return True if the attribute is a number type, false otherwise.
    */
   fun isNumberType(attribute: Attribute<*, *>): Boolean =
      Number::class.java.isAssignableFrom(attribute.javaType) ||
         (attribute.javaType.isPrimitive && attribute.javaType in PRIMITIVE_NUMBER_CLASSES) ||
         attribute.javaType.kotlin in KOTLIN_NUMBER_TYPES

   /**
    * Determines if the given attribute is of boolean type.
    *
    * @param attribute the attribute to check
    * @return true if the attribute is of boolean type, false otherwise
    */
   fun isBooleanType(attribute: Attribute<*, *>): Boolean {
      return Boolean::class.java.isAssignableFrom(attribute.javaType)
         || (attribute.javaType.isPrimitive && attribute.javaType == Boolean::class.javaPrimitiveType)
   }

   /**
    * Returns the name of the ID attribute for the given entity type.
    *
    * @param entityType the entity type
    * @return the name of the ID attribute
    */
   fun getIdAttributeName(entityType: EntityType<*>): String {
      // Get the declared Id attribute of the entity
      val idAttribute = entityType.getDeclaredId(entityType.idType.javaType)

      // Return the name of the ID attribute
      return idAttribute.name
   }

   /**
    * Finds an attribute by the given path in the provided entity type.
    *
    * @param entityType the entity type to search for the attribute
    * @param path the path of the attribute separated by dots
    * @return the found attribute or throws an exception if not found
    * @throws IllegalArgumentException if a non-association attribute is encountered before reaching the end of the path
    * @throws IllegalArgumentException if no attribute is found for the given path
    */
   fun findAttributeByPath(entityType: EntityType<*>, path: String): Attribute<*, *> {
      val parts = path.split(DOT)
      var currentType: ManagedType<*> = entityType
      var currentAttribute: Attribute<*, *>? = null

      for (part in parts) {
         currentAttribute = currentType.getAttribute(part)
         if (currentAttribute.isAssociation) {
            val attributeType = when (currentAttribute.persistentAttributeType) {
               Attribute.PersistentAttributeType.MANY_TO_ONE,
               Attribute.PersistentAttributeType.ONE_TO_ONE,
               -> (currentAttribute as SingularAttribute<*, *>).type

               Attribute.PersistentAttributeType.ONE_TO_MANY,
               Attribute.PersistentAttributeType.MANY_TO_MANY,
               -> (currentAttribute as PluralAttribute<*, *, *>).elementType

               else -> null
            }

            if (attributeType is ManagedType<*>) {
               currentType = attributeType
            } else {
               // We've encountered an attribute that does not navigate to an entity
               throw IllegalArgumentException("Non-association attribute encountered before the end of the path: $part")
            }
         } else {
            // If we're at the end of the path, we return the found attribute
            if (parts.last() == part) {
               return currentAttribute
            } else {
               // We've encountered a non-association attribute before reaching the end of the path
               throw IllegalArgumentException("Non-association attribute encountered before the end of the path: $part")
            }
         }
      }

      if (currentAttribute == null) {
         throw IllegalArgumentException("Could not find attribute for path: $path")
      }

      return currentAttribute
   }

   /**
    * Checks if the given path is a valid path for the specified entity type.
    *
    * @param entityType The entity type to validate against.
    * @param path The path to validate.
    * @return True if the path is valid, false otherwise.
    */
   fun isValidPath(entityType: EntityType<*>, path: String): Boolean {
      val parts = path.split(PERIOD)
      var currentType: ManagedType<*> = entityType

      for (part in parts) {
         val currentAttribute: Attribute<*, *>? = try {
            currentType.getAttribute(part)
         } catch (e: IllegalArgumentException) {
            // Attribute not found
            return false
         }

         if (currentAttribute != null) {
            if (currentAttribute.isAssociation) {
               val attributeType = when (currentAttribute.persistentAttributeType) {
                  Attribute.PersistentAttributeType.MANY_TO_ONE,
                  Attribute.PersistentAttributeType.ONE_TO_ONE,
                  -> (currentAttribute as SingularAttribute<*, *>).type

                  Attribute.PersistentAttributeType.ONE_TO_MANY,
                  Attribute.PersistentAttributeType.MANY_TO_MANY,
                  -> (currentAttribute as PluralAttribute<*, *, *>).elementType

                  else -> return false // Invalid or unsupported attribute type
               }

               if (attributeType is ManagedType<*>) {
                  currentType = attributeType
               } else {
                  // Encountered an attribute that does not navigate to an entity
                  return false
               }
            } else {
               // If we're at the end of the path, the path is valid
               if (parts.last() == part) {
                  return true
               } else {
                  // Non-association attribute encountered before the end of the path
                  return false
               }
            }
         }
      }

      // If we've traversed the entire path without issues, the path is valid
      return true
   }


   /**
    * Returns the inverse attribute for a given source attribute in a target entity.
    *
    * @param sourceEntityType The EntityType of the source entity.
    * @param sourceAttribute The source Attribute.
    * @param targetEntityType The EntityType of the target entity.
    * @return The inverse Attribute in the target entity.
    * @throws IllegalArgumentException if the inverse attribute cannot be found.
    */
   fun getInverseAttribute(
      sourceEntityType: EntityType<*>,
      sourceAttribute: Attribute<*, *>,
      targetEntityType: EntityType<*>,
   ): Attribute<*, *> {
      // Attempt to determine the mappedBy value from the source attribute's annotation.
      val mappedBy = getMappedByFromAnnotation(sourceAttribute.javaMember)

      // If mappedBy is not null or blank, find the attribute in the target entity that it refers to.
      if (!mappedBy.isNullOrBlank()) {
         return targetEntityType.getAttribute(mappedBy)
      } else {
         // If mappedBy is blank, this might be the owning side, so we need to find which attribute on the target entity has the mappedBy set to this source attribute's name.
         targetEntityType.attributes.forEach { targetAttribute ->
            if (targetAttribute.isAssociation && getMappedByFromAnnotation(targetAttribute.javaMember) == sourceAttribute.name) {
               return targetAttribute
            }
         }
      }

      throw IllegalArgumentException(
         "Could not find an inverse attribute for " +
            "'${sourceEntityType.name}.${sourceAttribute.name}' in entity '${targetEntityType.name}'."
      )
   }

   private fun getMappedByFromAnnotation(member: Member): String? {
      return when (member) {
         is Field -> {
            member.getAnnotation(OneToMany::class.java)?.mappedBy
               ?: member.getAnnotation(ManyToMany::class.java)?.mappedBy
               ?: member.getAnnotation(OneToOne::class.java)?.mappedBy
         }

         is Method -> {
            member.getAnnotation(OneToMany::class.java)?.mappedBy
               ?: member.getAnnotation(ManyToMany::class.java)?.mappedBy
               ?: member.getAnnotation(OneToOne::class.java)?.mappedBy
         }

         else -> null
      }
   }


}