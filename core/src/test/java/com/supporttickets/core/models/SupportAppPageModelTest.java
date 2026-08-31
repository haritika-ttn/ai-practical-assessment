package com.supporttickets.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportAppPageModelTest {

    @Test
    void exposesApiConfiguration() {
        SupportAppPageModel model = new SupportAppPageModel();
        assertEquals("/bin/support-tickets", model.getApiBase());
        assertEquals("/libs/granite/csrf/token.json", model.getCsrfTokenUrl());
        assertEquals("/content/support-app.html", model.getListPageUrl());
    }
}
