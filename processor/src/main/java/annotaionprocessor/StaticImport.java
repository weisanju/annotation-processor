package annotaionprocessor;

import com.squareup.javapoet.*;

import java.io.IOException;
import java.util.Collections;

public class StaticImport {
    public static void main(String[] args) throws IOException {
        ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
        ClassName list = ClassName.get("java.util", "List");
        ClassName arrayList = ClassName.get("java.util", "ArrayList");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, hoverboard);

        MethodSpec today = MethodSpec.methodBuilder("tomorrow")
                .returns(hoverboard)
                .addStatement("return new $T()", hoverboard)
                .build();

        ClassName namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards");

        MethodSpec beyond = MethodSpec.methodBuilder("beyond")
                .returns(listOfHoverboards)
                .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
                .addStatement("result.add($T.createNimbus(2000))", hoverboard)
                .addStatement("result.add($T.createNimbus(\"2001\"))", hoverboard)
                .addStatement("result.add($T.createNimbus($T.THUNDERBOLT))", hoverboard, namedBoards)
                .addStatement("$T.sort(result)", Collections.class)
                .addStatement("return result.isEmpty() ? $T.emptyList() : result", Collections.class)
                .build();

        TypeSpec hello = TypeSpec.classBuilder("HelloWorld")
                .addMethod(beyond)
                .build();

        JavaFile createNimbus = JavaFile.builder("com.example.helloworld", hello)
//                .addStaticImport(hoverboard, "createNimbus")
                .addStaticImport(namedBoards, "*")
                .addStaticImport(Collections.class, "*")
                .build();

        createNimbus.writeTo(System.out);
    }
}
