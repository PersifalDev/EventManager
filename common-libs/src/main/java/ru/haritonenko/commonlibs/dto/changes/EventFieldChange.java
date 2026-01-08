package ru.haritonenko.commonlibs.dto.changes;

public record EventFieldChange<T>(
        T oldField,
        T newField
) {
}
