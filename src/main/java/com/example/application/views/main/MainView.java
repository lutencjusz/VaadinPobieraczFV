package com.example.application.views.main;

import com.example.application.ThreadTest;
import com.example.application.model.Test;
import com.example.application.model.TestStatus;
import com.example.application.repo.InMemoRep;
import com.example.application.utils.Broadcaster;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.AROUND;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.START;


@PageTitle("Tests")
@Route(value = "")
@StyleSheet("/style.css")
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    private final int NOTIFICATION_DURATION_IN_MIN_SEC = 2000;
    Registration broadcasterRegistration;
    @Autowired
    InMemoRep inMemoRep = new InMemoRep();
    LocalDate date;
    Html servicesCounter;

    Image imagePng;

    Grid<Test> grid;

    UI ui;

    @Override
    protected void onAttach(AttachEvent attachEvent) {

        int REFRESH_INTERVAL_UI_IN_MIN_SEC = 3000;

        ui = getUI().isPresent() ? getUI().get() : null;
        date = LocalDate.now();
        servicesCounter = new Html("<b>Początkowa ilość serwisów: " + inMemoRep.getTests().size() + "</b>");
        HorizontalLayout logo = new HorizontalLayout();
        Image image = new Image("/icon.png", "Logo");
        image.setHeight("10%");
        image.setWidth("10%");
        image.setId("logo");

        DatePicker datePicker = new DatePicker("Wybierz datę wystawienia faktury:");
        datePicker.setId("dataPicker");
        datePicker.setValue(date);
        DatePicker.DatePickerI18n polishI18nDatePicker = new DatePicker.DatePickerI18n();
        polishI18nDatePicker.setMonthNames(List.of("Styczeń", "Luty", "Marzec", "Kwiecień",
                "Maj", "Czerwiec", "Lipiec", "Sierpień", "Wrzesień", "Październik",
                "Listopad", "Grudzień"));
        polishI18nDatePicker.setWeekdays(List.of("Niedziela", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela"));
        polishI18nDatePicker.setWeekdaysShort(List.of("Nie", "Pon", "Wto", "Śro", "Czw", "Pią", "Sob"));
        polishI18nDatePicker.setWeek("Tydzień");
        polishI18nDatePicker.setToday("Dzisiaj");
        polishI18nDatePicker.setCancel("Wróć");
        polishI18nDatePicker.setDateFormat("dd/MM/yyyy");
        polishI18nDatePicker.setFirstDayOfWeek(1);
        datePicker.setI18n(polishI18nDatePicker);
        datePicker.getElement().setProperty("title", "Data wygenerowania FV, istotna przy pobieraniu z Fakturowni (rok i miesiąc)");
        datePicker.addValueChangeListener(datePickerLocalDateComponentValueChangeEvent -> date = datePickerLocalDateComponentValueChangeEvent.getValue());

        logo.add(
                image,
                new H2("Pobieracz faktur"),
                datePicker
        );

        grid = new Grid<>(Test.class, false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(inMemoRep.getTests());
        grid.addSelectionListener(selection -> System.out.printf("Ilość zaznaczonych testów: %s%n", selection.getAllSelectedItems().size()));

        grid.addComponentColumn(this::createStatusBadge).setHeader("Status").setKey("status").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Test::getName).setHeader("Nazwa serwisu").setKey("name").setFooter(servicesCounter).setResizable(true)
                .getElement().setProperty("title", "Nazwa serwisu, z którego jest pobierana FV");
        grid.addColumn(Test::getNrFv).setHeader("Numer FV").setKey("nrfv").setResizable(true).getElement().setProperty("title", "Numer ostatniej pobranej FV");
        grid.addColumn(Test::getDropboxLink).setHeader("Link do Dropbox").setKey("link");
        grid.addColumn(new LocalDateRenderer<>(Test::getEstimatedDeliveryDate, "dd/MM/yyyy"))
                .setSortable(true).setHeader("Estymowana data dostarczenia")
                .setKey("date")
                .getElement().setProperty("title", "Ostatnia data pobrania FV");
        grid.addComponentColumn(this::createImagePng).setKey("screenshot").setHeader("Screeny");
        grid.addComponentColumn(test -> createButtons(test, inMemoRep.getTests(), grid)).setHeader("Akcje");

        grid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS,
                GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setColumnReorderingAllowed(true);

        grid.getColumnByKey("name")
                .setSortable(true);
        grid.getColumnByKey("status")
                .setSortable(true);
        grid.getColumnByKey("link")
                .setSortable(false);
        grid.setHeight("500px");

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setPadding(false);
        buttons.setJustifyContentMode(AROUND);

        Button addButton = new Button("Dodaj test");
        addButton.setId("addButton");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.getElement().setProperty("title", "Dodaje nowy test do zestawienia. Test musi być wcześniej napisany i skonfigurowany w systemie");

        Button cancelButton = new Button("Usuń wszystko");
        cancelButton.setId("cancelButton");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        cancelButton.getElement().setProperty("title", "Usuwa wszystkie wpisy z zestawienia, wpisy pojawią się po wciśnięciu 'Przywróć ustawienia'");

        Button initButton = new Button("Przywróć ustawienia");
        initButton.setId("initButton");
        initButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        initButton.getElement().setProperty("title", "Przywraca początkowe ustawienia wszystkich wpisów");

        Button executeTestsButton = new Button("Wykonaj testy");
        executeTestsButton.setId("executeTestsButton");
        executeTestsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        executeTestsButton.getElement().setProperty("title", "Wykonuje zaznaczone testy oraz te usunięte, które były zaznaczone przed usunięciem");

        buttons.add(executeTestsButton);
        buttons.add(addButton);
        buttons.add(cancelButton);
        buttons.add(initButton);

        cancelButton.addClickListener(buttonClickEvent -> {
            inMemoRep.clear();
            refreshItems();
        });

        initButton.addClickListener(buttonClickEvent -> {
            inMemoRep.clear();
            inMemoRep.initData();
            refreshItems();
        });

        executeTestsButton.addClickListener(buttonClickEvent -> {
            int testsNumber = grid.getSelectedItems().size();
            Notification notification;
            if (testsNumber > 0) {
                notification = Notification.show("Uruchomiono testów: " + grid.getSelectedItems().size());
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.setPosition(Notification.Position.TOP_CENTER);
                executionTests(grid.getSelectedItems());
            } else {
                notification = Notification.show("Nie wybrano żadnych testów do uruchomienia");
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.setPosition(Notification.Position.MIDDLE);
            }
        });

        addButton.addClickListener(buttonClickEvent -> {
            Dialog dialog = createTest();
            dialog.open();
        });

        executeTestsButton.addClickShortcut(Key.ENTER);
        refreshItems();

        add(
                logo,
                grid,
                buttons
        );

        ui.setPollInterval(REFRESH_INTERVAL_UI_IN_MIN_SEC);

        broadcasterRegistration = Broadcaster.register(message -> {
            System.out.println("Test '" + message + "' się zakończył i odświeżam grid");
            try {
                ui.access(() -> {
                    UI.setCurrent(ui);
                    refreshItems();
                    Notification finish = Notification.show("Zakończono pobieranie FV dla " + message);
                    finish.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    finish.setPosition(Notification.Position.TOP_CENTER);
                    finish.addOpenedChangeListener(not -> refreshItems());
                    finish.setDuration(NOTIFICATION_DURATION_IN_MIN_SEC);
                    finish.setOpened(true);
                });
            } catch (UIDetachedException e) {
                System.out.println("Nastąpił wyjątek w " + message);
            }
        });
    }

    public void refreshItems() {
        grid.setItems(inMemoRep.getTests());
        grid.getDataProvider().refreshAll();
    }

    public Image createImagePng(Test test) {

        String linkScreen = test.getStatus().equals(TestStatus.pass) ? "/png/" + test.getName().toLowerCase() + ".png" : "Brak_obrazka.png";

        imagePng = new Image(linkScreen, "screen shot");
        imagePng.setWidth("50px");
        imagePng.setHeight("50px");
        imagePng.addClickListener(imageClickEvent -> openZoomImageDialog(linkScreen).open());
        imagePng.getElement().setProperty("title", "Screen pokazujący dostępne FV w serwisie " + test.getName());
        return imagePng;
    }

    private Span createStatusBadge(Test test) {
        String theme;
        String statusToolTipDesc = "Status: ";
        switch (test.getStatus().toString()) {
            case "todo":
                theme = "badge primary";
                statusToolTipDesc += "TODO - test nie został uruchomiony, dane z poprzedniego testu";
                break;
            case "pass":
                theme = "badge success primary";
                statusToolTipDesc += "PASS - test wykonany pozytywnie, dane są aktualne";
                break;
            case "fail":
                theme = "badge error primary";
                statusToolTipDesc += "FAIL - test nie zakończył się pozytywnie, dane z poprzedniego testu";
                break;
            default:
                theme = "badge contrast primary";
                statusToolTipDesc += "test jest w trakcie wykonywania...";
                break;
        }
        Span badge = new Span(test.getStatus().toString().toUpperCase());
        badge.getStyle().set("width", "80px");
        badge.getElement().getThemeList().add(theme);
        badge.getElement().setProperty("title", statusToolTipDesc);
        createImagePng(test);
        return badge;
    }

    private Dialog createTest() {

        Dialog addTestDialog = new Dialog();
        addTestDialog.setHeaderTitle("Dodaj test");

        VerticalLayout addTestLayout = new VerticalLayout();

        TextField name = new TextField("Nazwa serwisu");
        TextField url = new TextField("Url");
        TextField nrFv = new TextField("Numer FV");
        TextField dropboxLink = new TextField("dropboxLink");
        Checkbox isInteractionNeed = new Checkbox("Czy wymaga wymiany informacji");

        addTestLayout.add(name, url, nrFv, dropboxLink, isInteractionNeed);

        Button cancelBtn = new Button("Anuluj");
        cancelBtn.addClickListener(buttonClickEvent -> addTestDialog.close());

        Button addBtn = new Button("Dodaj");
        addBtn.addClickListener(buttonClickEvent -> {
            inMemoRep.add(new Test(name.getValue(), url.getValue(), nrFv.getValue(), dropboxLink.getValue(), LocalDate.now(), TestStatus.todo, isInteractionNeed.getValue()));
            addTestDialog.close();
            refreshItems();
        });
        addTestDialog.add(addTestLayout);
        addTestDialog.getFooter().add(addBtn, cancelBtn);

        return addTestDialog;
    }

    private Dialog openZoomImageDialog(String fvLink) {
        Dialog zoomImageDialog = new Dialog();

        Button cancelZoomButton = new Button("Zamknij");
        cancelZoomButton.addClickListener(buttonClickEvent -> zoomImageDialog.close());

        Image zoomImage = new Image(fvLink, "FV");
        zoomImage.setHeight("100%");
        zoomImage.setWidth("100%");

        zoomImageDialog.add(zoomImage);
        zoomImageDialog.getFooter().add(cancelZoomButton);

        return zoomImageDialog;
    }

    private HorizontalLayout createButtons(Test test, List<Test> tests, Grid<Test> grid) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        /* Przycisk usuwający test */
        Button trashButton = new Button();
        trashButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR,
                ButtonVariant.LUMO_TERTIARY);
        trashButton.addClickListener(e -> {
            tests.remove(test);
            grid.getDataProvider().refreshAll();
        });
        trashButton.setIcon(new Icon(VaadinIcon.TRASH));
        trashButton.getElement().setProperty("title", "Usuwa pojedynczy wpis z panelu dla serwisu " + test.getName() + ". Test zostanie przywrócony po wciśnięciu 'Przywróć ustawienia'");

        /* Przycisk uruchamiający pojedynczy test */
        Button testButton = new Button();
        testButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_TERTIARY);
        testButton.addClickListener(e -> {
            Set<Test> oneTest = new HashSet<>();
            oneTest.add(test);
            executionTests(oneTest);
            refreshItems();
        });
        testButton.getElement().setProperty("title", "Uruchomienie testu tylko dla serwisu " + test.getName());
        testButton.setIcon(new Icon(VaadinIcon.PLAY_CIRCLE));
        testButton.setEnabled(!test.getStatus().equals(TestStatus.progress));

        /* link poglądu FV*/
        String linkScreen = "/fv/" + test.getName() + "_" + test.getNrFv().replace("/", "-").trim() + ".pdf";
        Anchor downloadPdf = new Anchor(linkScreen);
        downloadPdf.setTarget("_blank");
        downloadPdf.setEnabled(test.getStatus().equals(TestStatus.pass));

        Button downloadPdfButton = new Button();
        downloadPdfButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                ButtonVariant.MATERIAL_CONTAINED, ButtonVariant.LUMO_TERTIARY);
        downloadPdfButton.setIcon(new Icon(VaadinIcon.DOWNLOAD));
        downloadPdfButton.getElement().setProperty("title", "Otwiera na osobnej zakładce FV(pdf) dla linku: " + linkScreen);

        downloadPdf.add(downloadPdfButton);

        /* Wskaźnik wymagania interakcji*/
        Button interactionButton = new Button();
        interactionButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                ButtonVariant.MATERIAL_CONTAINED, ButtonVariant.LUMO_TERTIARY);
        interactionButton.setIcon(new Icon(VaadinIcon.EXCHANGE));
        interactionButton.getElement().setProperty("title", "Serwis S" + test.getName() + " wymaga wprowadzenia danych przez konsolę");

        /* Dodanie przycisków do layoutu */
        horizontalLayout.add(downloadPdf, testButton, trashButton);
        if (test.isInteractionNeed()) {
            horizontalLayout.add(interactionButton);
        }
        horizontalLayout.setJustifyContentMode(START);
        horizontalLayout.setSpacing(false);
        return horizontalLayout;
    }

    private void executionTests(Set<Test> tests) {
        for (Test test : tests) {
            ThreadTest testThread = new ThreadTest(test, date);
            testThread.start();
            refreshItems();
            Notification notification = Notification.show("Uruchomiono test: " + test.getName());
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.setPosition(Notification.Position.TOP_CENTER);
            notification.addOpenedChangeListener(not -> refreshItems());
            notification.setDuration(NOTIFICATION_DURATION_IN_MIN_SEC);
            notification.setOpened(true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }
}
