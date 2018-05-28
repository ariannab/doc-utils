package main.extractor;

import static java.util.stream.Collectors.toList;
import static main.extractor.DocumentedExecutable.*;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPrivateModifier;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.JavadocBlockTag.Type;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

import main.util.Reflection;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

/**
 * {@code JavadocExtractor} extracts {@code DocumentedExecutable}s from a Java class by means of
 * {@code extract(String, String)}. Uses both .java files and .class files: it obtains executable
 * members by means of reflection, then maps each reflection executable member to its corresponding
 * source member.
 */
public final class JavadocExtractor {

  /**
   * Returns a list of {@code DocumentedExecutable}s extracted from the class with name {@code
   * className}. Parses the Java source code of the specified class ({@code className}), and stores
   * information about the executable members of the specified class (including the Javadoc
   * comments).
   *
   * @param className the qualified class name of the class from which to extract documentation;
   *     must be on the classpath
   * @param sourcePath the path to the project source root folder
   * @return a list of documented executable members
   * @throws ClassNotFoundException if some reflection information cannot be loaded
   * @throws FileNotFoundException if the source code of the class with name {@code className}
   *     cannot be found in path {@code sourcePath}
   */
  public DocumentedType extract(String className, String sourcePath){

    //    log.info("Extracting Javadoc information of {} (in source folder {})", className, sourcePath);
    // Obtain executable members (constructors and methods) by means of reflection.
    final Class<?> clazz;
    try {
      clazz = Reflection.getClass(className);
    } catch (Throwable e) {
      return null;
    }
//    final List<Executable> reflectionExecutables = getExecutables(clazz);

    // Obtain executable members (constructors and methods) in the source code.
    final ImmutablePair<String, String> fileNameAndSimpleName =
        getFileNameAndSimpleName(clazz, className);
    final String fileName = fileNameAndSimpleName.getLeft();
    final String sourceFile =
        sourcePath + File.separator + fileName.replaceAll("\\.", File.separator) + ".java";
    final String simpleName = fileNameAndSimpleName.getRight();
    final List<CallableDeclaration<?>> sourceExecutables = getExecutables(simpleName, sourceFile);

    if(!sourceExecutables.isEmpty()) {
      // Maps each reflection executable member to its corresponding source executable member.
//    Map<Executable, CallableDeclaration<?>> executablesMap =
//        mapExecutables(reflectionExecutables, sourceExecutables, className);

      // Create the list of ExecutableMembers.
      List<String> classesInPackage = getClassesInSamePackage(className, sourceFile);
      List<DocumentedExecutable> documentedExecutables =
              new ArrayList<>(sourceExecutables.size());
      for (CallableDeclaration<?> sourceCallable : sourceExecutables) {
        final List<DocumentedParameter> parameters =
                createDocumentedParameters(
                        sourceCallable.getParameters());
//      final String qualifiedClassName = reflectionMember.getDeclaringClass().getName();
        BlockTags blockTags =
                null;
        try {
          blockTags = createTags(classesInPackage, sourceCallable, parameters);
        } catch (ClassNotFoundException e) {
          return null;
        }

        final Optional<JavadocComment> javadocComment = sourceCallable.getJavadocComment();
        final Optional<Javadoc> javadoc = sourceCallable.getJavadoc();
        String freeText = "";
        String parsedFreeText = "";
        if (javadocComment.isPresent()) {
//        freeText = String.valueOf(javadocComment.get());
          freeText = javadocComment.get().getContent();
          String[] freeTextLines = freeText.split("\n");
          for (String line : freeTextLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("* @since") || trimmedLine.startsWith("* @param") ||
                    trimmedLine.startsWith("* @return") || trimmedLine.startsWith("* @throws")) {
              break;
            } else if (trimmedLine.startsWith("* ")) {
              parsedFreeText += trimmedLine.substring(2, trimmedLine.length());
            }

          }
        }
        documentedExecutables.add(new DocumentedExecutable(
                sourceCallable.getName(), sourceCallable.getSignature(), parameters, blockTags, parsedFreeText));
      }

      //    log.info(
      //        "Extracting Javadoc information of {} (in source folder {}) done", className, sourcePath);

      // Create the documented class.

      return new DocumentedType(clazz, documentedExecutables);
    }
    return null;
  }

  private ImmutablePair<String, String> getFileNameAndSimpleName(Class<?> clazz, String className) {
    String fileName;
    String simpleName;
    final int dollarPosition = className.indexOf("$");
    if (dollarPosition != -1) {
      // Nested class: source file won't match the name.
      fileName = className.substring(0, dollarPosition);
      simpleName =
          className.substring(className.lastIndexOf(".") + 1, dollarPosition + 1)
              + clazz.getSimpleName();
    } else {
      // Top-level class.
      fileName = className;
      simpleName = clazz.getSimpleName();
    }
    return ImmutablePair.of(fileName, simpleName);
  }

  /**
   * Returns the list of class names found in the same package of {@code className}. We need them to
   * map the {@code Exception}s declared in the Javadoc with their corresponding classes.
   *
   * @param className String name of the class for which to find classes in same package
   * @param sourceFile path of the class source file
   * @return list of String holding the qualified class names found in folder
   */
  private List<String> getClassesInSamePackage(String className, String sourceFile) {
    // TODO Improve the code: this method should return all the available types in a given package.
    // TODO Replace string manipulation by using data structures
    String packagePath = sourceFile.substring(0, sourceFile.lastIndexOf("/"));
    File folder = new File(packagePath);
    File[] listOfFiles = folder.listFiles();
    List<String> classesInPackage = new ArrayList<>();
    for (File file : listOfFiles) {
      if (!file.getName().endsWith(".java")) {
        continue;
      }
      // This loop examines the .java files in the same directory as the .java class being analysed
      // in order to find eventual Exception classes located in the same package.
      // "package-info" files are not useful for this purpose.
      String name = getClassNameForSource(file.getName(), className);
      if (name != null && !name.equals(className) && !name.contains("package-info")) {
        classesInPackage.add(name);
      }
    }
    return classesInPackage;
  }

  /**
   * Given the simple file name of a source located in the same package of the Class being analysed
   * and the name of the Class itself, composes the qualified class name corresponding to the
   * source.
   *
   * @param sourceFileName name of the source file found in package
   * @param analyzedClassName qualified class name of the class being analysed
   * @return the qualified class name of the source or null if {@code analyzedClassName} does not
   *     contain a dot (i.e., {@code analyzedClassName} is a simple, not-qualified name)
   */
  private String getClassNameForSource(String sourceFileName, String analyzedClassName) {
    int lastDot = analyzedClassName.lastIndexOf(".");
    if (lastDot == -1) {
      return null;
    }
    return analyzedClassName.substring(0, lastDot) + "." + sourceFileName.replace(".java", "");
  }

  /**
   * Creates tags (of param, return or throws kind) referred to a callable member.
   *
   * @param classesInPackage list of class names in sourceCallable's package
   * @param callableMember the callable member the tags refer to
   * @param parameters {@code sourceCallable}'s parameters
//   * @param className qualified name of the class defining {@code sourceCallable}
   * @return a triple of created tags: list of @param tags, return tag, list of @throws tags
   * @throws ClassNotFoundException if a type described in a Javadoc comment cannot be loaded (e.g.,
   *     the type is not on the classpath)
   */
  private BlockTags createTags(
      List<String> classesInPackage,
      CallableDeclaration<?> callableMember,
      List<DocumentedParameter> parameters)
      throws ClassNotFoundException {

    List<ParamTag> paramTags = new ArrayList<>();
    ReturnTag returnTag = null;
    List<ThrowsTag> throwsTags = new ArrayList<>();

    final Optional<Javadoc> javadocOpt = callableMember.getJavadoc();
    if (javadocOpt.isPresent()) {
      final Javadoc javadocComment = javadocOpt.get();
      final List<JavadocBlockTag> blockTags = javadocComment.getBlockTags();
      for (JavadocBlockTag blockTag : blockTags) {
        switch (blockTag.getType()) {
          case PARAM:
            final ParamTag paramTag = createParamTag(blockTag, parameters);
            if (paramTag != null) { // Ignore @param comments describing generic type parameters.
              paramTags.add(paramTag);
            }
            break;
          case RETURN:
            returnTag = createReturnTag(blockTag);
            break;
          case EXCEPTION:
          case THROWS:
            throwsTags.add(createThrowsTag(classesInPackage, blockTag, callableMember));
            break;
          default:
            // ignore other block tags
            break;
        }
      }
    }
    return new BlockTags(paramTags, returnTag, throwsTags);
  }

  /**
   * Create a tag of throws kind.
   *
   * @param classesInPackage list of class names in sourceCallable's package
   * @param blockTag the @throws or @exception Javadoc block comment containing the tag
   * @param sourceCallable the source callable the tag refers to
//   * @param className qualified name of the class defining {@code sourceCallable}
   * @return the created tag
   * @throws ClassNotFoundException if the class of the exception type couldn't be found
   */
  private ThrowsTag createThrowsTag(
      List<String> classesInPackage,
      JavadocBlockTag blockTag,
      CallableDeclaration<?> sourceCallable)
      throws ClassNotFoundException {
    // Javaparser library does not provide a nice parsing of @throws tags. We have to parse the
    // comment text by ourselves.
    final Type blockTagType = blockTag.getType();
    if (!blockTagType.equals(Type.THROWS) && !blockTagType.equals(Type.EXCEPTION)) {
      throw new IllegalArgumentException(
          "The block tag " + blockTag + " does not refer to an" + " @throws or @exception tag");
    }

    String comment = blockTag.getContent().toText();
    final String[] tokens = comment.split("[\\s\\t]+", 2);
    final String exceptionName = tokens[0];
//    Class<?> exceptionType =
//        findExceptionType(classesInPackage, sourceCallable, exceptionName, className);
    String commentToken = "";
    if (tokens.length > 1) {
      //a tag can report the exception type even without any description
      commentToken = tokens[1];
    }
    Comment commentObject = new Comment(commentToken);
    return new ThrowsTag(commentObject, exceptionName);
  }

  /**
   * Create a tag of return kind.
   *
   * @param blockTag the @return block containing the tag
   * @return the created tag
   */
  private ReturnTag createReturnTag(JavadocBlockTag blockTag) {
    final Type blockTagType = blockTag.getType();
    if (!blockTagType.equals(Type.RETURN)) {
      throw new IllegalArgumentException(
          "The block tag " + blockTag + " does not refer to an" + " @return tag");
    }

    String content = blockTag.getContent().toText();
    // Fix bug in Javaparser: missing open bracket of {@code} inline tag.
    if (content.startsWith("@code ")) {
      content = "{" + content;
    }

    Comment commentObject = new Comment(content);
    return new ReturnTag(commentObject);
  }

  /**
   * Create a tag of param kind.
   *
   * @param blockTag the block containing the tag
   * @param parameters the formal parameter list in which looking for the one associated to the tag
   * @return the created tag, null if {@code blockTag} refers to a @param tag documenting a generic
   *     type parameter.
   */
  private ParamTag createParamTag(JavadocBlockTag blockTag, List<DocumentedParameter> parameters) {
    final Type blockTagType = blockTag.getType();
    if (!blockTagType.equals(Type.PARAM)) {
      throw new IllegalArgumentException(
          "The block tag " + blockTag + " does not refer to an" + " @param tag");
    }

    String paramName = blockTag.getName().orElse("");

    // Return null if blockTag refers to a @param tag documenting a generic type parameter.
    if (paramName.startsWith("<") && paramName.endsWith(">")) {
      return null;
    }

    String finalParamName = paramName;
    final List<DocumentedParameter> matchingParams =
        parameters.stream().filter(p -> p.getName().equals(finalParamName)).collect(toList());
    // TODO If paramName not present in paramNames => issue a warning about incorrect documentation!
    // TODO If more than one matching parameter found => issue a warning about incorrect documentation!
    Comment commentObject = new Comment(blockTag.getContent().toText());
    if(matchingParams.isEmpty()){
      paramName = "nullParamName";
    }
    return new ParamTag(paramName, commentObject);
  }

  /**
   * Instantiate the {@code DocumentedParameter} according to the list of source parameters.
   *
   * @param sourceParams the {@code NodeList} of parameters found in source
   * @return the list of {@code org.toradocu.main.extractor.DocumentedParameter}
   */
  private List<DocumentedParameter> createDocumentedParameters(
      NodeList<com.github.javaparser.ast.body.Parameter> sourceParams) {

    List<DocumentedParameter> parameters = new ArrayList<>(sourceParams.size());
    for (int i = 0; i < sourceParams.size(); i++) {
      final com.github.javaparser.ast.body.Parameter parameter = sourceParams.get(i);
      final String paramName = parameter.getName().asString();
      final Boolean nullable = isNullable(parameter);
      parameters.add(new DocumentedParameter(paramName, nullable));
    }
    return parameters;
  }

  /**
   * Checks whether the given parameter is annotated with @NotNull or @Nullable or similar.
   *
   * @param parameter the parameter to check
   * @return true if the parameter is annotated with @Nullable, false if the parameter is annotated
   *     with @NonNull, null otherwise or if it's both nullable and notNull
   */
  private Boolean isNullable(com.github.javaparser.ast.body.Parameter parameter) {
    final List<String> parameterAnnotations =
        parameter.getAnnotations().stream().map(a -> a.getName().asString()).collect(toList());
    List<String> notNullAnnotations = new ArrayList<>(parameterAnnotations);
    notNullAnnotations.retainAll(DocumentedParameter.notNullAnnotations);
    List<String> nullableAnnotations = new ArrayList<>(parameterAnnotations);
    nullableAnnotations.retainAll(DocumentedParameter.nullableAnnotations);

    if (!notNullAnnotations.isEmpty() && !nullableAnnotations.isEmpty()) {
      // Parameter is annotated as both nullable and notNull.

      return null;
    }
    if (!notNullAnnotations.isEmpty()) {
      return false;
    }
    if (!nullableAnnotations.isEmpty()) {
      return true;
    }
    return null;
  }

  /**
   * Collects non-private non-synthetic constructors and methods through reflection. Notice that
   * this method considers compiler-generated class initialization methods (e.g., default
   * constructors) as non-synthetic (<a
   * href="http://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">JLS 13.1, item
   * 11</a>).
   *
   * @param clazz the Class containing the executables to collect
   * @return non private-executables of {@code clazz}
   */
  private List<Executable> getExecutables(Class<?> clazz) {
    try {
      List<Executable> executables = new ArrayList<>();
      executables.addAll(Arrays.asList(clazz.getDeclaredConstructors()));
      executables.addAll(Arrays.asList(clazz.getDeclaredMethods()));
      executables.removeIf(Executable::isSynthetic);
      executables.removeIf(e -> Modifier.isPrivate(e.getModifiers())); // Ignore private members.
      return executables;
    }catch(Exception e){
      //nothing
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Collects non-private callables from source code.
   *
   * @param className the String class name
   * @param sourcePath the String source path
   * @return non private-callables of the class with name {@code className}
   * @throws FileNotFoundException if the source path couldn't be resolved
   */
  public List<CallableDeclaration<?>> getExecutables(String className, String sourcePath) {
    final List<CallableDeclaration<?>> sourceExecutables = new ArrayList<>();
    try {
      final ClassOrInterfaceDeclaration sourceClass = getClassDefinition(className, sourcePath);
      if (sourceClass == null) {
        return Collections.EMPTY_LIST;
      }
      sourceExecutables.addAll(sourceClass.getConstructors());
      sourceExecutables.addAll(sourceClass.getMethods());
      sourceExecutables.removeIf(NodeWithPrivateModifier::isPrivate); // Ignore private members.
      return Collections.unmodifiableList(sourceExecutables);
    }catch(Exception e){
      //nothing
      return Collections.EMPTY_LIST;
    }
  }

  private ClassOrInterfaceDeclaration getClassDefinition(String className, String sourcePath)
      throws FileNotFoundException {
    final CompilationUnit cu = JavaParser.parse(new File(sourcePath));

    String nestedClassName = "";
    if (className.contains("$")) {
      // Nested class.
      nestedClassName = className.substring(className.indexOf("$") + 1, className.length());
      className = className.substring(0, className.indexOf("$"));
    }

    Optional<ClassOrInterfaceDeclaration> definitionOpt = cu.getClassByName(className);
    if (!definitionOpt.isPresent()) {
      definitionOpt = cu.getInterfaceByName(className);
    }

    if (definitionOpt.isPresent()) {
      if (!nestedClassName.isEmpty()) {
        // Nested class.
        NodeList<BodyDeclaration<?>> containingClassMembers = definitionOpt.get().getMembers();
        for (BodyDeclaration<?> childNode : containingClassMembers) {
          if (childNode instanceof ClassOrInterfaceDeclaration
              && ((ClassOrInterfaceDeclaration) childNode)
                  .getName()
                  .asString()
                  .equals(nestedClassName)) {
            return (ClassOrInterfaceDeclaration) childNode;
          }
        }
      } else {
        // Top-level class or interface.
        return definitionOpt.get();
      }
    }
    Optional<EnumDeclaration> enumDefinitionOpt = cu.getEnumByName(className);
    if(enumDefinitionOpt.isPresent()){
      return null;
    }
    throw new IllegalArgumentException(
        "Impossible to find a class or interface with name " + className + " in " + sourcePath);
  }

//  /**
//   * Maps reflection executable members to source code executable members.
//   *
//   * @param reflectionExecutables the list of reflection members
//   * @param sourceExecutables the list of source code members
//   * @param className name of the class containing the executables
//   * @return a map holding the correspondences
//   */
//  private Map<Executable, CallableDeclaration<?>> mapExecutables(
//      List<Executable> reflectionExecutables,
//      List<CallableDeclaration<?>> sourceExecutables,
//      String className) {
//
//    filterOutGeneratedConstructors(reflectionExecutables, sourceExecutables, className);
//
//    if (reflectionExecutables.size() != sourceExecutables.size()) {
//      // TODO Add the differences to the error message to better characterize the error.
//      throw new IllegalArgumentException("Error: Provided lists have different size.");
//    }
//
//    Map<Executable, CallableDeclaration<?>> map = new LinkedHashMap<>(reflectionExecutables.size());
//    for (CallableDeclaration<?> sourceCallable : sourceExecutables) {
//      final List<Executable> matches =
//          reflectionExecutables
//              .stream()
//              .filter(
//                  e ->
//                      getSimpleNameOfExecutable(e.getName(), e instanceof Constructor)
//                              .equals(sourceCallable.getName().asString())
//                          && sameParamTypes(e.getParameters(), sourceCallable.getParameters()))
//              .collect(toList());
//      if (matches.size() < 1) {
//        throw new AssertionError(
//            "Cannot find reflection executable member corresponding to "
//                + sourceCallable.getSignature());
//      }
//      if (matches.size() > 1) {
//        throw new AssertionError(
//            "Found multiple reflection executable members corresponding to "
//                + sourceCallable.getSignature());
//      }
//      map.put(matches.get(0), sourceCallable);
//    }
//    return map;
//  }

  /**
   * Removes compiler-generated constructors from {@code reflectionExecutables}.
   *
   * @param reflectionExecutables executable members obtained via reflection
   * @param sourceExecutables executable members obtained parsing the source code
   * @param className name of the class containing the executables
   */
  private void filterOutGeneratedConstructors(
      List<Executable> reflectionExecutables,
      List<CallableDeclaration<?>> sourceExecutables,
      String className) {
    final List<CallableDeclaration<?>> sourceConstructors =
        sourceExecutables
            .stream()
            .filter(e -> e instanceof ConstructorDeclaration && e.getParameters().isEmpty())
            .collect(toList());

    List<Executable> reflectionConstructors;
    Predicate<Executable> filterPredicate;
    if (!className.contains("$")) {
      filterPredicate = e -> e instanceof Constructor && e.getParameterCount() == 0;
    } else {
      String containingClassName = className.substring(0, className.indexOf("$"));
      filterPredicate =
          e ->
              e instanceof Constructor
                  && e.getParameterCount() == 1
                  && e.getParameters()[0].getType().getName().equals(containingClassName);
    }
    reflectionConstructors =
        reflectionExecutables.stream().filter(filterPredicate).collect(toList());

    for (Executable reflectionConstructor : reflectionConstructors) {
      final String reflectionConstructorName = reflectionConstructor.getName();
      final String reflectionConstructorSimpleName =
          reflectionConstructorName.substring(reflectionConstructorName.lastIndexOf(".") + 1);
      boolean inSource = false;
      for (CallableDeclaration<?> sourceConstructor : sourceConstructors) {
        if (sourceConstructor.getNameAsString().equals(reflectionConstructorSimpleName)) {
          inSource = true;
        }
      }
      if (!inSource) {
        reflectionExecutables.remove(reflectionConstructor);
      }
    }
  }

  /**
   * Checks that reflection param types and source param types are the same.
   *
   * @param reflectionParams array of reflection param types
   * @param sourceParams NodeList of source param types
   * @return true if the param types are the same, false otherwise
   */
  private boolean sameParamTypes(
      java.lang.reflect.Parameter[] reflectionParams,
      NodeList<com.github.javaparser.ast.body.Parameter> sourceParams) {
    if (reflectionParams.length != sourceParams.size()) {
      return false;
    }

    for (int i = 0; i < reflectionParams.length; i++) {
      final Parameter reflectionParam = reflectionParams[i];
      String reflectionQualifiedTypeName =
          rawType(reflectionParam.getParameterizedType().getTypeName());
      //TODO Replace the next 18 lines, which do string manipulation, by code that uses data structures
      if (reflectionParam.isVarArgs() && !reflectionQualifiedTypeName.endsWith("[]")) {
        // Sometimes var args type name ends with "[]", sometimes don't. That's why we need this
        // check.
        reflectionQualifiedTypeName += "[]";
      }
      reflectionQualifiedTypeName = reflectionQualifiedTypeName.replaceAll("\\$", ".");

      final com.github.javaparser.ast.body.Parameter sourceParam = sourceParams.get(i);
      String sourceTypeName = rawType(sourceParam.getType().asString());
      if (sourceParam.isVarArgs()) {
        sourceTypeName += "[]";
      }

      boolean sameType;
      if (sourceTypeName.contains(".")) {
        // Here we cannot test for equality: Consider the case where source type is "b.c" while
        // reflection type is "a.b.c". Equality is a too strict condition.
        sameType = reflectionQualifiedTypeName.endsWith(sourceTypeName);
      } else {
        sameType = (getSimpleName(reflectionQualifiedTypeName)).equals(sourceTypeName);
      }
      if (!sameType) {
        return false;
      }
    }

    return true;
  }

  /**
   * Get raw type corresponding to a possibly-generic type; that is, remove generic type arguments.
   *
   * @param type string representation of the type
   * @return the raw type
   */
  private String rawType(String type) {
    int i = type.indexOf("<");
    if (i != -1) { // If type contains "<".
      return type.substring(0, i);
    }
    return type;
  }

  /**
   * Given the name of an {@code Executable} and the information about whether it is a constructor
   * or not, the method returns the name as it is or the name without the class prefix (which is
   * present in constructor's name).
   *
   * @param name String name to parse
   * @param isConstructor tells whether the {@code Executable} is a constructor
   * @return the String simple name without prefixes
   */
  private String getSimpleNameOfExecutable(String name, boolean isConstructor) {
    int dollar = name.indexOf("$");
    if (isConstructor && dollar == -1) {
      // Constructor name is prefixed with class name, which is removed
      name = getSimpleName(name);
    }
    // For nested classes, the simple name of the constructor is found after the dollar sign
    if (isConstructor && dollar != -1) {
      name = name.substring(dollar + 1, name.length());
    }

    return name;
  }

  /**
   * Given a qualified type name, removes the prefixes to obtain the simple name.
   *
   * @param type the String type qualified name
   * @return the String simple name
   */
  private String getSimpleName(String type) {
    // Constructor names contain package name.
    int lastDot = type.lastIndexOf(".");
    if (lastDot != -1) {
      type = type.substring(lastDot + 1);
    }

    return type;
  }

  /**
   * Search for the type of the exception with the given type name. The type name is allowed to be
   * fully-qualified or simple, in which case this method tries to guess the package name.
   *
   * @param classesInPackage list of class names in {@code sourceCallable}'s package
   * @param sourceCallable the callable for which the exception with type name {@code
   *     exceptionTypeName} is expected
   * @param exceptionTypeName the exception type name (can be fully-qualified or simple)
   * @param className package of the class where {@code sourceCallable} is defined
   * @return the exception class
   * @throws ClassNotFoundException if exception class couldn't be loaded
   */
//  private Class<?> findExceptionType(
//      List<String> classesInPackage,
//      CallableDeclaration<?> sourceCallable,
//      String exceptionTypeName,
//      String className)
//      throws ClassNotFoundException {
//
//    if(exceptionTypeName.endsWith(",")){
//      exceptionTypeName = exceptionTypeName.substring(0, exceptionTypeName.length()-1);
//    }
//    try {
//      return Reflection.getClass(exceptionTypeName);
//    } catch (ClassNotFoundException e) {
//      // Intentionally empty: Apply other heuristics to load the exception type.
//    }
//    // Try to load a nested class.
//    try {
//      return Reflection.getClass(className + "$" + exceptionTypeName);
//    } catch (ClassNotFoundException e) {
//      // Intentionally empty: Apply other heuristics to load the exception type.
//    }
//    try {
//      return Reflection.getClass("java.lang." + exceptionTypeName);
//    } catch (ClassNotFoundException e) {
//      // Intentionally empty: Apply other heuristics to load the exception type.
//    }
//    try {
//      return Reflection.getClass("java.io." + exceptionTypeName);
//    } catch (ClassNotFoundException e) {
//      // Intentionally empty: Apply other heuristics to load the exception type.
//    }
//    try {
//      return Reflection.getClass("java.util." + exceptionTypeName);
//    } catch (ClassNotFoundException e) {
//      // Intentionally empty: Apply other heuristics to load the exception type.
//    }
//    // Look in classes of package.
//    for (String classInPackage : classesInPackage) {
//      if (classInPackage.contains(exceptionTypeName)) {
//        // TODO Add a comment explaining why the following check is needed.
//        if (classInPackage.contains("$")) {
//          classInPackage = classInPackage.replace(".class", "");
//        }
//        return Reflection.getClass(classInPackage);
//      }
//    }
//    // Look for an import statement to complete exception type name.
//    CompilationUnit cu = getCompilationUnit(sourceCallable);
//    final NodeList<ImportDeclaration> imports = cu.getImports();
//    for (ImportDeclaration anImport : imports) {
//      String importedType = anImport.getNameAsString();
//     if(anImport.isAsterisk()) {
//       Reflections reflections =
//               new Reflections(importedType, new SubTypesScanner(false));
//
//       //TODO this works for java libraries e.g. java.util.* but not classes of PUT
//        Set<Class<? extends Object>> allClasses =
//                reflections.getSubTypesOf(Object.class);
//        for(Class<? extends Object> aClass : allClasses){
//          if (aClass.getName().endsWith("."+exceptionTypeName)) {
//
//          }
//        }
//      }
//      if (importedType.endsWith("."+exceptionTypeName)) {
//        //if the class name of the exception is preceeded by another class name (not package, because of
//        //the capital letter) then the exception is a nested class
//          String prefix = importedType.substring(0, importedType.indexOf("." + exceptionTypeName));
//          int indexBeforeExceptionType = prefix.lastIndexOf(".");
//          String nameBeforeExcepType = prefix.substring(indexBeforeExceptionType + 1, prefix.length());
//          if (Character.isUpperCase(nameBeforeExcepType.charAt(0))) {
//            importedType = importedType.replace("." + exceptionTypeName, "$" + exceptionTypeName);
//          }
//        return Reflection.getClass(importedType);
//      }
//    }
//    // TODO Improve error message.
//    throw new ClassNotFoundException(
//        "Unable to load exception type " + exceptionTypeName + ". Is it on the classpath?");
//  }

  /**
   * Returns the compilation unit where {@code callableMember} is defined.
   *
   * @param callableMember a callable member
   * @return the compilation unit where {@code callableMember} is defined
   */
  private CompilationUnit getCompilationUnit(CallableDeclaration<?> callableMember) {
    Optional<Node> nodeOpt = callableMember.getParentNode();
    if (!nodeOpt.isPresent()) {
      throw new NullPointerException(
          "Unexpected null value for parent of "
              + callableMember.getSignature()
              + ", cannot get the compilation unit where it is defined");
    }

    Node node = nodeOpt.get();
    while (!(node instanceof CompilationUnit)) {
      nodeOpt = node.getParentNode();
      if (!nodeOpt.isPresent()) {
        throw new NullPointerException(
            "Unexpected null value for parent of "
                + callableMember.getSignature()
                + ", cannot get the compilation unit where it is defined");
      }
      node = nodeOpt.get();
    }
    return (CompilationUnit) node;
  }
}
