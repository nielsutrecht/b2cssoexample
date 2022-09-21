package com.nibado.example.b2csso;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@Controller
@SessionAttributes("state")
public class IndexController {
    private final SsoComponent component;

    public IndexController(SsoComponent component) {
        this.component = component;
    }

    @GetMapping
    public ModelAndView index(HttpServletRequest request) throws Exception {
        var url = component.getRedirectUrl(createOrGetState(request));
        return new ModelAndView("index", Map.of("url", url));
    }

    private String createOrGetState(HttpServletRequest request) {
        if(request.getSession().getAttribute("state") == null) {
            request.getSession().setAttribute("state", UUID.randomUUID().toString());
        }
        return request.getSession().getAttribute("state").toString();
    }

    @GetMapping("/redirect")
    public ModelAndView redirect(HttpServletRequest request,
                                 @RequestParam(name = "code", required = false) String code,
                                 @RequestParam(name = "error_description", required = false) String error,
                                 @RequestParam(name = "state", required = false) String state) throws Exception {

        if (error != null) {
            System.out.println(error);
            return new ModelAndView("error", Map.of("error", code));
        }

        if(state == null) {
            return new ModelAndView("error", Map.of("error", "State should not be null"));
        }

        if(code == null) {
            return new ModelAndView("error", Map.of("error", "Code should not be null"));
        }

        if(!state.equals(createOrGetState(request))) {
            return new ModelAndView("error", Map.of("error", "Session State does not match"));
        }

        var userObjectId = component.handleCallback(code);

        return new ModelAndView("redirect", Map.of("code", code, "oid", userObjectId));
    }
}
