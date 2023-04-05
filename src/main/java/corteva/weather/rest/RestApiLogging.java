package corteva.weather.rest;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.time.*;

/**
 * A Filter that logs rest response data,
 */
@Component
@WebFilter("/api/*")
@Slf4j
public class RestApiLogging implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        Instant start = Instant.now();
        try {
            chain.doFilter(req, resp);
        } finally {
            Instant finish = Instant.now();
            long timeMs = Duration.between(start, finish).toMillis();
            log.info(String.format(
                    "%d %s %s %,d ms",
                    ((HttpServletResponse) resp).getStatus(),
                    ((HttpServletRequest) req).getMethod(),
                    ((HttpServletRequest) req).getRequestURI(),
                    timeMs
            ));
        }
    }

    @Override
    public void destroy() {
    }
}