package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Gọi service tài khoản F1 qua hợp đồng tại commit 0f4e3de, không truy cập repository F1.
 * Nhánh F2 vẫn biên dịch độc lập; chỉ đăng ký bean khi F1 có đủ ba phương thức.
 * Luôn gọi bean do Spring quản lý để giữ giao dịch của AuthService trong giao dịch F2.
 */
@Component("f1MemberAccountAdapter")
@Conditional(F1MemberAccountAdapter.AccountServiceAvailable.class)
public class F1MemberAccountAdapter implements MemberAccountAccess {
    static final String F1_SERVICE = "vn.edu.hust.nhom6.gym.feature.auth.service.AuthService";

    private final Supplier<?> service;
    private final Method emails;
    private final Method disable;
    private final Method activate;

    @Autowired
    public F1MemberAccountAdapter(ApplicationContext context) {
        this(() -> context.getBean(ClassUtils.resolveClassName(F1_SERVICE, context.getClassLoader())),
            ClassUtils.resolveClassName(F1_SERVICE, context.getClassLoader()));
    }

    F1MemberAccountAdapter(Supplier<?> service, Class<?> contract) {
        this.service = service;
        emails = method(contract, "getEmailsByUserIds", Map.class, Collection.class);
        disable = method(contract, "disableAccount", void.class, Long.class);
        activate = method(contract, "activateAccountIfDisabled", void.class, Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, String> emails(Collection<Long> userIds) {
        return (Map<Long, String>) invoke(emails, userIds);
    }

    @Override
    public void disable(Long userId) { invoke(disable, userId); }

    @Override
    public void activateIfDisabled(Long userId) { invoke(activate, userId); }

    private Object invoke(Method method, Object argument) {
        try {
            return method.invoke(service.get(), argument);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException cause) { throw cause; }
            if (ex.getCause() instanceof Error cause) { throw cause; }
            throw new IllegalStateException("Dịch vụ tài khoản F1 phát sinh lỗi ngoài hợp đồng.", ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Không thể gọi dịch vụ tài khoản F1.", ex);
        }
    }

    private static Method method(Class<?> contract, String name, Class<?> result, Class<?> parameter) {
        try {
            Method method = contract.getMethod(name, parameter);
            if (method.getReturnType() != result) { throw new NoSuchMethodException(name); }
            return method;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Hợp đồng dịch vụ tài khoản F1 chưa tương thích: " + name, ex);
        }
    }

    static boolean supports(Class<?> contract) {
        try {
            method(contract, "getEmailsByUserIds", Map.class, Collection.class);
            method(contract, "disableAccount", void.class, Long.class);
            method(contract, "activateAccountIfDisabled", void.class, Long.class);
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    static class AccountServiceAvailable implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return ClassUtils.isPresent(F1_SERVICE, context.getClassLoader())
                && supports(ClassUtils.resolveClassName(F1_SERVICE, context.getClassLoader()));
        }
    }
}
