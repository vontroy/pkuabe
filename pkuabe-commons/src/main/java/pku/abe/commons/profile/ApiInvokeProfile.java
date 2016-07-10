package pku.abe.commons.profile;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;

/**
 * Created by LinkedME05 on 16/5/12.
 */
@Component
@Aspect
public class ApiInvokeProfile {
    // public * pku.abe.api.*(..)
    //第一个*代表返回值为任意, .*代表api包下的所有子包,(..)代表包下的各种方法
    @Around("(execution(public * pku.abe.api.resources.*.*(..)) " +
            "|| execution(public * pku.abe.api.lkme.web.*.*.*(..))) && @annotation(path)")
    public Object apiAround(ProceedingJoinPoint pjp, Path path) throws Throwable{
        String type = ProfileType.API.value();
        Path root = pjp.getTarget().getClass().getAnnotation(Path.class);
        String uri = catpath(root, path);
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            throw e;
        }finally{
            long end = System.currentTimeMillis();
            long cost = end - start;
            ProfileUtil.accessStatistic(type, uri, end, cost);
        }
    }

    public static String catpath(Path root,Path path ){
        String rootValue = root.value();
        StringBuilder uri = new StringBuilder();
        if(!rootValue.startsWith("/")){
            uri.append('/');
        }
        uri.append(rootValue);
        if(rootValue.endsWith("/")){
            uri.deleteCharAt(uri.length()-1);
        }
        String pathValue = path.value();
        if(!pathValue.startsWith("/")){
            uri.append('/');
        }
        uri.append(pathValue);
        return uri.toString();
    }

}