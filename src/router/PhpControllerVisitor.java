package router;

import beans.ArrayKeyVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;

import java.util.List;

public class PhpControllerVisitor extends PsiRecursiveElementWalkingVisitor {

    private final String prefix;
    private final ArrayKeyVisitor visitor;

    public PhpControllerVisitor(String prefix, ArrayKeyVisitor visitor) {
        this.prefix = prefix;
        this.visitor = visitor;
    }

    //递归遍历psi元素
    @Override
    public void visitElement(PsiElement element) {
        if (element instanceof PhpClass) {
            PsiElement[] childrens = element.getChildren();
            for (PsiElement item : childrens) {
                if (item instanceof Method) {
                    Method method = (Method) item;
                    String route = this.prefix + "/" + method.getName();
                    this.visitor.visit(route, method, false);
                }
            }
        } else {
            super.visitElement(element);
        }
    }
}
