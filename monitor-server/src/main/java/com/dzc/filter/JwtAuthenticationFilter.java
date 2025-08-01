package com.dzc.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.dzc.entity.RestBean;
import com.dzc.entity.dto.Account;
import com.dzc.entity.dto.Client;
import com.dzc.service.AccountService;
import com.dzc.service.ClientService;
import com.dzc.utils.Const;
import com.dzc.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    JwtUtils utils;

    @Resource
    ClientService clientService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String uri=request.getRequestURI();
        if(uri.startsWith("/monitor")){
            if(!uri.endsWith("/register")){
                Client client=clientService.findClientByToken(authorization);
                if(client==null){
                    response.setStatus(401);
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(RestBean.failure(401,"未注册").asJsonString());
                }else {
                    request.setAttribute(Const.ATTR_CLIENT,client);
                }
            }
        }else{
            DecodedJWT jwt = utils.resolveJwt(authorization);
            if(jwt != null) {
                UserDetails user = utils.toUser(jwt);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                int id = utils.toId(jwt);
                request.setAttribute(Const.ATTR_USER_ID,id);
                request.setAttribute(Const.ATTR_USER_ROLE,new ArrayList<>(user.getAuthorities()).get(0).getAuthority());
            }
        }
        filterChain.doFilter(request, response);
    }
}
