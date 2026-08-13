package dev.creategmf.diagnostics;

/** A nearby Create block-entity type and its object count. This is not a timing measurement. */
public record BlockEntityLoad(String typeName, int objects) {
}
