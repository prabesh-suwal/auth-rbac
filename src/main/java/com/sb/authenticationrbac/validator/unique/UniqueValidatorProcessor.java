//package com.sb.authenticationrbac.validator.unique;
//
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.annotation.processing.SupportedSourceVersion;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.type.TypeMirror;
//import javax.tools.Diagnostic;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//@SupportedAnnotationTypes("com.sb.authenticationrbac.validator.unique.UniqueValidator")
//@SupportedSourceVersion(SourceVersion.RELEASE_17)
//public class UniqueValidatorProcessor extends AbstractProcessor {
//
//    private final Map<String, String> seen = new HashMap<>();
//
//    @Override
//    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        for (Element e : roundEnv.getElementsAnnotatedWith(UniqueValidator.class)) {
//            if (!(e instanceof TypeElement)) continue;
//
//            TypeElement type = (TypeElement) e;
//            List<? extends TypeMirror> interfaces = type.getInterfaces();
//
//            for (TypeMirror iface : interfaces) {
//                String raw = iface.toString();
//                System.out.println("========================== HERE =========================");
//                if (raw.startsWith("com.sb.authenticationrbac.validator.unique.Validator<")) {
//                    String generic = raw.replace("com.sb.authenticationrbac.validator.unique.Validator<", "").replace(">", "");
//
//                    if (seen.containsKey(generic)) {
//                        processingEnv.getMessager().printMessage(
//                                Diagnostic.Kind.ERROR,
//                                "Duplicate Validator for type: " + generic +
//                                        ". Already defined in: " + seen.get(generic),
//                                e
//                        );
//                    } else {
//                        seen.put(generic, type.getQualifiedName().toString());
//                    }
//                }
//            }
//        }
//        return true;
//    }
//}
