package annotaionprocessor;

import com.google.auto.service.AutoService;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.tools.javac.util.List.nil;

/**
 *
 */
@AutoService(Processor.class)
public class JucTreeProcessor extends AbstractProcessor {

    private static final String METHOD_PREFIX = "start";

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;

    public JucTreeProcessor() {
        System.out.println(1);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        ProcessingEnvironment unwrappedprocessingEnv = jbUnwrap(ProcessingEnvironment.class, processingEnvironment);
        super.init(unwrappedprocessingEnv);
        filer = unwrappedprocessingEnv.getFiler();
        messager = unwrappedprocessingEnv.getMessager();
        trees = JavacTrees.instance(unwrappedprocessingEnv);
        Context context = ((JavacProcessingEnvironment) unwrappedprocessingEnv).getContext();
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }
    private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        }
        catch (Throwable ignored) {}
        return unwrapped != null? unwrapped : wrapper;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        roundEnvironment.getElementsAnnotatedWith(Data.class).forEach(element -> {
            JCTree jcTree = trees.getTree(element);
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    //获取所有的变量
                    Map<String,Integer> skipInfos = new HashMap<>();
                    String toString = "toString";

                    List<JCTree.JCVariableDecl> jcVariableDeclList = nil();
                    for (JCTree tree : jcClassDecl.getMembers()) {
                        if (tree.getKind().equals(Tree.Kind.METHOD)) {
                            JCTree.JCMethodDecl method =(JCTree.JCMethodDecl) tree;
                            com.sun.tools.javac.util.Name methodName = method.getName();
                            if (methodName.toString().equals(toString)) {
                                skipInfos.merge(toString,1,Integer::sum);
                                continue;
                            }

                            com.sun.tools.javac.util.Name getter = names.fromString("get");
                            if (methodName.startsWith(getter)) {
                                String s = methodName.toString();
                                String s1 = s.substring(3, 4).toLowerCase() + s.substring(4);
                                skipInfos.merge(s1,2,Integer::sum);
                                continue;
                            }
                            com.sun.tools.javac.util.Name setter = names.fromString("set");

                            if (methodName.startsWith(setter)) {
                                String s = methodName.toString();
                                String s1 = s.substring(3, 4).toLowerCase() + s.substring(4);
                                skipInfos.merge(s1,4,Integer::sum);
                                continue;
                            }
                        }

                        if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                            jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                        }
                    }
                    if ((skipInfos.getOrDefault(toString,0)&1)== 0) {
                        jcClassDecl.defs = jcClassDecl.defs.prepend(generatorToString(jcVariableDeclList));
                    }

                    for (JCTree.JCVariableDecl jcVariableDecl : jcVariableDeclList) {
                        if ((skipInfos.getOrDefault(jcVariableDecl.name.toString(),0)&2) == 0) {
                            jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                        }
                        if ((skipInfos.getOrDefault(jcVariableDecl.name.toString(),0)&4) == 0) {
                            jcClassDecl.defs = jcClassDecl.defs.prepend(makeSetterMethodDecl(jcVariableDecl));
                        }
                    }
                    super.visitClassDef(jcClassDecl);
                }

            });
        });
        return true;
    }


    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        com.sun.tools.javac.util.Name name = jcVariableDecl.getName();
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), name)));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        return treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC),
                getNewMethodName(name),
                jcVariableDecl.vartype,
                nil(),
                nil(),
                nil(),
                body,
                null);
    }
    private JCTree.JCMethodDecl makeSetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        JCTree.JCIdent aThis = treeMaker.Ident(names.fromString("this"));
        com.sun.tools.javac.util.Name variableName = jcVariableDecl.name;
        statements.append(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(aThis,variableName),treeMaker.Ident(variableName))));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        JCTree.JCVariableDecl param3 = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), variableName,jcVariableDecl.vartype, null);
        com.sun.tools.javac.util.List<JCTree.JCVariableDecl> parameters3 = com.sun.tools.javac.util.List.of(param3);
        return treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC),
                setNewMethodName(variableName),
                treeMaker.Type(new Type.JCVoidType()),
                nil(),
                parameters3,
                nil(),
                body,
                null);
    }
    private JCTree.JCMethodDecl generatorToString(java.util.List<JCTree.JCVariableDecl> jcVariableDeclList){
        if (jcVariableDeclList.isEmpty()) {
            return null;
        }
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        JCTree.JCIdent aThis = treeMaker.Ident(names.fromString("this"));
        JCTree.JCBinary binary = null;
        for (JCTree.JCVariableDecl jcVariableDecl : jcVariableDeclList) {
            JCTree.JCBinary binary1 = treeMaker.Binary(JCTree.Tag.PLUS, treeMaker.Select(aThis, jcVariableDecl.getName()), treeMaker.Literal("@@_@@"));
            if (binary!=null) {
                binary = treeMaker.Binary(JCTree.Tag.PLUS,
                        binary,
                        binary1
                        );
            }else{
                binary = binary1;
            }
        }
        statements.append(treeMaker.Return(binary));

        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());

        return treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC),
                names.fromString("toString"),
                treeMaker.Ident(names.fromString(String.class.getSimpleName()))
                ,
                nil(),
                nil(),
                nil(),
                body,
                null);
    }

    /**
     * 获取新方法名，get + 将第一个字母大写 + 后续部分, 例如 value 变为 getValue
     *
     * @param name
     * @return
     */
    private com.sun.tools.javac.util.Name getNewMethodName(Name name) {
        String s = name.toString();
        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }
    private com.sun.tools.javac.util.Name setNewMethodName(Name name) {
        String s = name.toString();
        return names.fromString("set" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> objects = new HashSet<>();
        objects.add(Data.class.getCanonicalName());
        return objects;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
