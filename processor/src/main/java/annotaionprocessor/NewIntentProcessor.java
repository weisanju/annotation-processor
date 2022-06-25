package annotaionprocessor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;

/**
 */
@AutoService(Processor.class)
public class NewIntentProcessor extends AbstractProcessor {

    private static final String METHOD_PREFIX = "start";

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Map<String, String> activitiesWithPackage;

    public NewIntentProcessor() {
        System.out.println(1);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elements = processingEnvironment.getElementUtils();
        activitiesWithPackage = new HashMap<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        try {
            /**
             * 1- Find all annotated element
             */
            for (Element element : roundEnvironment.getElementsAnnotatedWith(NewIntent.class)) {

                if (element.getKind() != ElementKind.CLASS) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
                    return true;
                }
                TypeElement typeElement = (TypeElement) element;
                activitiesWithPackage.put(
                        typeElement.getSimpleName().toString(),
                        elements.getPackageOf(typeElement).getQualifiedName().toString());
            }

//
//            /**
//             * 2- Generate a class
//             */
//            TypeSpec.Builder navigatorClass = TypeSpec
//                    .classBuilder("Navigator")
//                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
//
//            for (Map.Entry<String, String> element : activitiesWithPackage.entrySet()) {
//                String activityName = element.getKey();
//                String packageName = element.getValue();
//                ClassName activityClass = ClassName.get(packageName, activityName);
//                MethodSpec intentMethod = MethodSpec
//                        .methodBuilder(METHOD_PREFIX + activityName)
//                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                        .returns(void.class)
//                        .addParameter(classContext, "context")
//                        .addStatement("$L.startActivity(new $T($L, $L))", "context", classIntent, "context", activityClass + ".class")
//                        .build();
//                navigatorClass.addMethod(intentMethod);
//            }
//
//
//            /**
//             * 3- Write generated class to a file
//             */
//            JavaFile.builder("com.annotationsample", navigatorClass.build()).build().writeTo(filer);

            MethodSpec today = MethodSpec.methodBuilder("today")
                    .returns(Date.class)
                    .addStatement("return new $T()", Date.class)
                    .build();

            TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(today)
                    .build();

            JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
                    .build();


//            javaFile.writeTo(filer);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> objects = new HashSet<>();
        objects.add(NewIntent.class.getCanonicalName());
        System.out.println(1);
        return objects;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()  {
        return SourceVersion.latestSupported();
    }

}
