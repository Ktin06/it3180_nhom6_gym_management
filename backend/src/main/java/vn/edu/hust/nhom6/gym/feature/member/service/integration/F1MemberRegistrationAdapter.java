package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;

/**
 * Nối đúng interface MemberService của F1 sang service tạo hồ sơ thật của F2.
 * Dùng FactoryBean để F2 vẫn biên dịch khi nhánh F1 chưa được gộp: kiểu sản phẩm
 * chính là interface F1 khi có trên classpath, không khai báo một bản interface F1 khác.
 * Bean thông thường này được ưu tiên hơn DefaultMemberService có @Fallback của F1.
 */
@Component("f2MemberRegistrationAdapter")
@ConditionalOnClass(name = F1MemberRegistrationAdapter.F1_SERVICE)
public class F1MemberRegistrationAdapter implements FactoryBean<Object> {
    static final String F1_SERVICE = "vn.edu.hust.nhom6.gym.feature.auth.service.MemberService";

    private final Class<?> contract;
    private final Object adapter;

    @Autowired
    public F1MemberRegistrationAdapter(ObjectProvider<MemberRegistrationService> registrations) {
        this(registrations, ClassUtils.resolveClassName(F1_SERVICE, ClassUtils.getDefaultClassLoader()));
    }

    F1MemberRegistrationAdapter(ObjectProvider<MemberRegistrationService> registrations, Class<?> contract) {
        this.contract = contract;
        Method registration = registrationMethod(contract);
        this.adapter = Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract},
            (proxy, method, args) -> {
                if (method.equals(registration)) {
                    // Lấy service qua Spring để giữ nguyên proxy @Transactional của F2.
                    registrations.getObject().registerMemberProfile((Long) args[0], (String) args[1],
                        (String) args[2], (LocalDate) args[3], (String) args[4]);
                    return null;
                }
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "F2 member registration adapter for " + contract.getName();
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
                throw new IllegalStateException("Hợp đồng tạo hồ sơ F1 đã thay đổi: " + method);
            });
    }

    private static Method registrationMethod(Class<?> contract) {
        if (!contract.isInterface()) {
            throw new IllegalStateException("Hợp đồng tạo hồ sơ F1 phải là interface.");
        }
        try {
            Method method = contract.getMethod("registerMemberProfile", Long.class, String.class,
                String.class, LocalDate.class, String.class);
            if (method.getReturnType() != void.class || contract.getMethods().length != 1) {
                throw new IllegalStateException("Hợp đồng tạo hồ sơ F1 không tương thích với F2.");
            }
            return method;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Hợp đồng tạo hồ sơ F1 không tương thích với F2.", ex);
        }
    }

    @Override
    public Object getObject() { return adapter; }

    @Override
    public Class<?> getObjectType() { return contract; }

    @Override
    public boolean isSingleton() { return true; }
}
