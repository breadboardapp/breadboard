package moe.apex.rule34.util


/** Opt-in to accessing fields that are deprecated and reserved for migrations.
    If this field is being accessed outside of a migration, you are doing something wrong.
    See the deprecation message for the replacement.*/
@RequiresOptIn(
    message = "This field should only be used in migrations.",
    level = RequiresOptIn.Level.ERROR
)
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class MigrationOnlyField
