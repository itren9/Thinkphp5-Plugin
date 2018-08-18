package view;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import inter.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.MethodMatcher;
import util.PsiElementUtil;
import util.PsiElementUtils;
import view.dict.TemplatePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

// view 处理类
public class ViewReferences2 implements GotoCompletionLanguageRegistrar {
    private static MethodMatcher.CallToSignature[] VIEWS = new MethodMatcher.CallToSignature[]{
            new MethodMatcher.CallToSignature("\\think\\View", "fetch"),
            new MethodMatcher.CallToSignature("\\think\\GotoController", "fetch"),
            new MethodMatcher.CallToSignature("\\think\\GotoController", "display"),
    };

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(PlatformPatterns.psiElement(), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@Nullable PsiElement psiElement) {
                if (psiElement == null) {// || !LaravelProjectComponent.isEnabled(psiElement)) {
                    return null;
                }

                PsiElement parent = psiElement.getParent();
                if (parent != null
                        && (PsiElementUtil.isFunctionReference(parent, "view", 0) || MethodMatcher.getMatchedSignatureWithDepth(parent, VIEWS) != null)
                        && handlePath(psiElement)) {
                    return new ViewDirectiveCompletionProvider(parent);
                }
                return null;
            }
        });
    }


    @Override
    public boolean support(@NotNull Language language) {
        return PhpLanguage.INSTANCE == language;
    }

    public boolean handlePath(PsiElement psiElement) {
        String application = "application";
        String projectPath = psiElement.getProject().getBasePath();//"D:\\project2\\test";
        String currentFilePath = psiElement.getContainingFile().getVirtualFile().getPath(); //"D:\\project2\\test\\application\\index\\controller\\Index.php";
        String[] arr = currentFilePath.replace(projectPath, "").split("/");
        if (arr.length < 4 || !arr[1].equals(application)) {
            return false;
        }
        String moduleName = arr[2];
        ViewCollector.DEFAULT_TEMPLATE_PATH = new TemplatePath[]{new TemplatePath(application + "/" + moduleName + "/view", false)};
        ViewCollector.curModule = moduleName;
        return true;
    }


    private static class ViewDirectiveCompletionProvider extends GotoCompletionProvider {
        private ViewDirectiveCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            final Collection<LookupElement> lookupElements = new ArrayList<>();

            ViewCollector.visitFile(getProject(), new ViewCollector.ViewVisitor() {
                        @Override
                        public void visit(@NotNull VirtualFile virtualFile, @NotNull String name) {
                            lookupElements.add(LookupElementBuilder.create(name).withIcon(virtualFile.getFileType().getIcon()));
                        }
                    }
            );

            // @TODO: no filesystem access in test; fake item
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                lookupElements.add(LookupElementBuilder.create("test_view"));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<? extends PsiElement> getPsiTargets(@NotNull PsiElement psiElement, int offset, @NotNull Editor editor) {
            PsiElement stringLiteral = psiElement.getParent();
            if (!(stringLiteral instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String contents = ((StringLiteralExpression) stringLiteral).getContents();
            if (StringUtils.isBlank(contents)) {
                Method method = PsiElementUtil.getMethod(psiElement);
                if (method == null)
                    return Collections.emptyList();
                else
                    contents = method.getName();
            }

            // select position of click event
            int caretOffset = offset - psiElement.getTextRange().getStartOffset();

            Collection<PsiElement> targets = new ArrayList<>(PsiElementUtils.convertVirtualFilesToPsiFiles(
                    getProject(),
                    TemplateUtil.resolveTemplate(getProject(), contents, caretOffset)
            ));

            // @TODO: no filesystem access in test; fake item
            if ("test_view".equals(contents) && ApplicationManager.getApplication().isUnitTestMode()) {
                targets.add(PsiManager.getInstance(getProject()).findDirectory(getProject().getBaseDir()));
            }

            return targets;
        }
    }
}