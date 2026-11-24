package com.example.demo.beans;

import com.example.demo.models.CargoEnum;
import jakarta.faces.model.SelectItem;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("authBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthBean {
    private final List<SelectItem> papeisCadastro = Arrays.stream(CargoEnum.values())
            .map(papel -> new SelectItem(papel.name(), papel.getDescricao()))
            .toList();

    public List<SelectItem> getPapeisCadastro() {
        return papeisCadastro;
    }
}
