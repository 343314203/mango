package mango.proxy;

import mango.common.URLParam;
import mango.core.DefaultRequest;
import mango.core.Response;
import mango.exception.RpcFrameworkException;
import mango.rpc.Invoker;
import mango.util.Constants;
import mango.util.RequestIdGenerator;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ReferenceInvocationHandler<T> implements InvocationHandler {
    private Invoker<T> invoker;

    public ReferenceInvocationHandler(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //toString,equals,hashCode,finalize等接口未声明的方法不进行远程调用
        if(method.getDeclaringClass().equals(Object.class)){
            if ("toString".equals(method.getName())) {
                return "";
            }
            throw new RpcFrameworkException("can not invoke local method:" + method.getName());
        }

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setArguments(args);
        request.setType(Constants.REQUEST_SYNC);
        //调用参数
        request.setAttachment(URLParam.version.getName(), invoker.getServiceUrl().getVersion());
        request.setAttachment(URLParam.group.getName(), invoker.getServiceUrl().getGroup());
        try {
            Response resp = invoker.call(request);
            return getValue(resp);
        } catch (RuntimeException e) {
            throw new RpcFrameworkException("invoke exception", e);
        }
    }

    public Object getValue(Response resp) {
        Exception exception = resp.getException();
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RpcFrameworkException(
                    exception.getMessage(), exception);
        }
        return resp.getResult();
    }
}
