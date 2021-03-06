package org.docutils.extractor;

import org.docutils.util.Checks;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Represents a class or interface that is documented with Javadoc comments. */
public final class DocumentedType {

  /** Documented class or interface (or enum, ...). */
  private final Class<?> documentedClass;
  /** Constructors and methods of this documented type. */
  private final List<DocumentedExecutable> documentedExecutables;
  /** The Javadoc class summary. **/
  private String classSummary;

  /**
   * Creates a new DocumentedType wrapping the given class and with the given constructors and
   * methods.
   *
   * @param documentedClass the {@code Class} of this documentedClass
   * @param documentedExecutables constructors and methods of {@code documentedClass}
   * @throws NullPointerException if either documentedClass or documentedExecutables is null
   */
  DocumentedType(Class<?> documentedClass, List<DocumentedExecutable> documentedExecutables) {
    Checks.nonNullParameter(documentedClass, "documentedClass");
    Checks.nonNullParameter(documentedExecutables, "documentedExecutables");
    this.documentedClass = documentedClass;
    this.documentedExecutables = documentedExecutables;
  }


  DocumentedType(Class<?> documentedClass, String classSummary) {
    Checks.nonNullParameter(documentedClass, "documentedClass");
    this.documentedExecutables = new ArrayList<DocumentedExecutable>();
    this.documentedClass = documentedClass;
    this.classSummary = classSummary;
  }

  /**
   * Returns the runtime class of the documented type this DocumentedType represents.
   *
   * @return the runtime class of the documented type this DocumentedType represents
   */
  Class<?> getDocumentedClass() {
    return documentedClass;
  }

  /**
   * Returns constructors and methods of this {@code DocumentedType}.
   *
   * @return constructors and methods of this {@code DocumentedType}
   */
  public List<DocumentedExecutable> getDocumentedExecutables() {
    return documentedExecutables;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DocumentedType)) {
      return false;
    }
    DocumentedType that = (DocumentedType) o;
    return documentedClass.equals(that.documentedClass)
        && documentedExecutables.equals(that.documentedExecutables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentedClass, documentedExecutables);
  }

    public String getClassSummary() {
      return this.classSummary;
    }
}
