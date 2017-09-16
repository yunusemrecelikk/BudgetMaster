package de.deadlocker8.budgetmaster.ui.controller;

import java.time.LocalDate;
import java.util.ArrayList;

import org.joda.time.DateTime;

import de.deadlocker8.budgetmaster.logic.category.Category;
import de.deadlocker8.budgetmaster.logic.payment.NormalPayment;
import de.deadlocker8.budgetmaster.logic.payment.Payment;
import de.deadlocker8.budgetmaster.logic.payment.RepeatingPayment;
import de.deadlocker8.budgetmaster.logic.payment.RepeatingPaymentEntry;
import de.deadlocker8.budgetmaster.logic.serverconnection.ExceptionHandler;
import de.deadlocker8.budgetmaster.logic.serverconnection.ServerConnection;
import de.deadlocker8.budgetmaster.logic.serverconnection.ServerTagConnection;
import de.deadlocker8.budgetmaster.logic.utils.Colors;
import de.deadlocker8.budgetmaster.logic.utils.Helpers;
import de.deadlocker8.budgetmaster.logic.utils.Strings;
import de.deadlocker8.budgetmaster.ui.Styleable;
import de.deadlocker8.budgetmaster.ui.cells.ButtonCategoryCell;
import de.deadlocker8.budgetmaster.ui.cells.RepeatingDayCell;
import de.deadlocker8.budgetmaster.ui.cells.SmallCategoryCell;
import de.deadlocker8.budgetmaster.ui.tagField.TagField;
import fontAwesome.FontIconType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import logger.Logger;
import tools.AlertGenerator;
import tools.ConvertTo;
import tools.Localization;

public class NewPaymentController extends BaseController implements Styleable
{
	@FXML private ScrollPane scrollPane;
	@FXML private VBox vboxContent;
	@FXML private TextField textFieldName;
	@FXML private TextField textFieldAmount;
	@FXML private Button buttonCancel;
	@FXML private Button buttonSave;
	@FXML private ComboBox<Category> comboBoxCategory;
	@FXML private DatePicker datePicker;
	@FXML private DatePicker datePickerEnddate;
	@FXML private Spinner<Integer> spinnerRepeatingPeriod;
	@FXML private ComboBox<Integer> comboBoxRepeatingDay;
	@FXML private CheckBox checkBoxRepeat;
	@FXML private RadioButton radioButtonPeriod;
	@FXML private RadioButton radioButtonDay;
	@FXML private Label labelText1, labelText2, labelText3;
	@FXML private TextArea textArea;
	@FXML private HBox hboxTags;

	private Stage parentStage;
	private Controller controller;
	private PaymentController paymentController;
	private boolean isPayment;
	private boolean edit;
	private Payment payment;
	private ButtonCategoryCell buttonCategoryCell;
	
	public NewPaymentController(Stage parentStage, Controller controller, PaymentController paymentController, boolean isPayment, boolean edit, Payment payment)
	{
		this.parentStage = parentStage;
		this.controller = controller;
		this.paymentController = paymentController;
		this.isPayment = isPayment;
		this.edit = edit;
		this.payment = payment;
		load("/de/deadlocker8/budgetmaster/ui/fxml/NewPaymentGUI.fxml", Localization.getBundle());
		getStage().showAndWait();
	}
	
	@Override
	public void initStage(Stage stage)
	{
		stage.initOwner(parentStage);
		stage.initModality(Modality.APPLICATION_MODAL);
		String titlePart;

		titlePart = isPayment ? Localization.getString(Strings.TITLE_PAYMENT) : Localization.getString(Strings.TITLE_INCOME);

		if(edit)
		{
			stage.setTitle(Localization.getString(Strings.TITLE_PAYMENT_EDIT, titlePart));
		}
		else
		{
			stage.setTitle(Localization.getString(Strings.TITLE_PAYMENT_NEW, titlePart));
		}
	
		stage.getIcons().add(controller.getIcon());
		stage.setResizable(false);
		stage.getScene().getStylesheets().add("/de/deadlocker8/budgetmaster/ui/style.css");
	}
	
	@Override
	public void init()
	{	
		vboxContent.prefWidthProperty().bind(scrollPane.widthProperty().subtract(25));
		
		applyStyle();
		
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 0);
		spinnerRepeatingPeriod.setValueFactory(valueFactory);
		spinnerRepeatingPeriod.setEditable(true);
		spinnerRepeatingPeriod.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if(!newValue)
			{
				spinnerRepeatingPeriod.increment(0); // won't change value, but will commit editor
			}
		});

		comboBoxRepeatingDay.setCellFactory((view) -> {
			return new RepeatingDayCell();
		});
		ArrayList<Integer> days = new ArrayList<>();
		for(int i = 1; i <= 31; i++)
		{
			days.add(i);
		}
		comboBoxRepeatingDay.getItems().addAll(days);
		
		comboBoxCategory.setCellFactory((view) -> {
			return new SmallCategoryCell();
		});
		comboBoxRepeatingDay.setValue(1);
		buttonCategoryCell = new ButtonCategoryCell(Color.WHITE);
		comboBoxCategory.setButtonCell(buttonCategoryCell);
		comboBoxCategory.setStyle("-fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
		comboBoxCategory.valueProperty().addListener((listener, oldValue, newValue) -> {		
			comboBoxCategory.setStyle("-fx-background-color: " + newValue.getColor() + "; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
			buttonCategoryCell.setColor(Color.web(newValue.getColor()));
		});

		checkBoxRepeat.selectedProperty().addListener((listener, oldValue, newValue) -> {
			toggleRepeatingArea(newValue);
		});

		comboBoxCategory.getItems().clear();
		try
		{
			ServerConnection connection = new ServerConnection(controller.getSettings());
			ArrayList<Category> categories = connection.getCategories();
			if(categories != null)
			{
				for(Category currentCategory : categories)
				{
					if(currentCategory.getID() != 2)
					{
						comboBoxCategory.getItems().add(currentCategory);
					}
				}
					
			}
		}
		catch(Exception e)
		{
			controller.showConnectionErrorAlert(ExceptionHandler.getMessageForException(e));
			getStage().close();
			return;
		}

		final ToggleGroup toggleGroup = new ToggleGroup();
		radioButtonPeriod.setToggleGroup(toggleGroup);
		radioButtonDay.setToggleGroup(toggleGroup);
		radioButtonPeriod.selectedProperty().addListener((listener, oldValue, newValue) -> {
			toggleRadioButtonPeriod(newValue);
		});

		datePickerEnddate.setDayCellFactory((p) -> new DateCell()
		{
			@Override
			public void updateItem(LocalDate ld, boolean bln)
			{
				super.updateItem(ld, bln);

				if(datePicker.getValue() != null && ld.isBefore(datePicker.getValue()))
				{
					setDisable(true);
					setStyle("-fx-background-color: #ffc0cb;");
				}
			}
		});	
		
		if(edit)
		{
			//prefill
			textFieldName.setText(payment.getName());
			textFieldAmount.setText(Helpers.NUMBER_FORMAT.format(Math.abs(payment.getAmount()/100.0)).replace(".", ","));		
			comboBoxCategory.setValue(controller.getCategoryHandler().getCategory(payment.getCategoryID()));
			datePicker.setValue(LocalDate.parse(payment.getDate()));
			textArea.setText(payment.getDescription());
			
			if(payment instanceof RepeatingPaymentEntry)
			{
				try
				{					
					RepeatingPaymentEntry currentPayment = (RepeatingPaymentEntry)payment;
					
					ServerConnection connection = new ServerConnection(controller.getSettings());
					RepeatingPayment repeatingPayment = connection.getRepeatingPayment(currentPayment.getRepeatingPaymentID());
					datePicker.setValue(LocalDate.parse(repeatingPayment.getDate()));
					
					//repeates every x days
					if(currentPayment.getRepeatInterval() != 0)
					{					
						checkBoxRepeat.setSelected(true);
						radioButtonPeriod.setSelected(true);
						toggleRepeatingArea(true);
						spinnerRepeatingPeriod.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, currentPayment.getRepeatInterval()));
					}
					//repeat every month on day x
					else
					{
						checkBoxRepeat.setSelected(true);
						radioButtonDay.setSelected(true);
						toggleRepeatingArea(true);
						comboBoxRepeatingDay.getSelectionModel().select(currentPayment.getRepeatMonthDay()-1);
					}
					if(currentPayment.getRepeatEndDate() != null)
					{
						datePickerEnddate.setValue(LocalDate.parse(currentPayment.getRepeatEndDate()));
					}
				}
				catch(Exception e)
				{
					Logger.error(e);
					controller.showConnectionErrorAlert(ExceptionHandler.getMessageForException(e));
				}
			}	
			else
			{				
				checkBoxRepeat.setSelected(false);
				radioButtonPeriod.setSelected(true);
				toggleRepeatingArea(false);
			}
			
			
			ServerTagConnection s;
			try
			{
				s = new ServerTagConnection(controller.getSettings());
				TagField tagField = new TagField(s.getAllTagsForPayment((NormalPayment)payment), s.getTags());
				hboxTags.getChildren().add(tagField);
				tagField.maxWidthProperty().bind(hboxTags.widthProperty());
				HBox.setHgrow(tagField, Priority.ALWAYS);
			}
			catch(Exception e)
			{
				//ERRORHANDLING
				Logger.error(e);
			}
		}
		else
		{
			comboBoxCategory.setValue(controller.getCategoryHandler().getCategory(1));
			checkBoxRepeat.setSelected(false);
			radioButtonPeriod.setSelected(true);
			toggleRepeatingArea(false);			

			//preselect correct month and year
			DateTime currentDate = controller.getCurrentDate();		
			if(DateTime.now().getDayOfMonth() > currentDate.dayOfMonth().withMaximumValue().getDayOfMonth())
			{
				currentDate = currentDate.dayOfMonth().withMaximumValue();				
			}
			
			LocalDate currentLocalDate = LocalDate.now().withYear(currentDate.getYear())
			.withMonth(currentDate.getMonthOfYear())
			.withDayOfMonth(currentDate.getDayOfMonth());
			datePicker.setValue(currentLocalDate);	
			datePickerEnddate.setValue(currentLocalDate);
		}
		
		datePicker.setEditable(false);
	}

	public void save()
	{
		String name = textFieldName.getText();
		if(name == null || name.equals(""))
		{
			AlertGenerator.showAlert(AlertType.WARNING,
			                        Localization.getString(Strings.TITLE_WARNING),
			                        "", 
			                        Localization.getString(Strings.WARNING_EMPTY_PAYMENT_NAME), 
			                        controller.getIcon(), 
			                        controller.getStage(), 
			                        null, 
			                        false);
			return;
		}
		
		if(name.length() > 150)
		{
			AlertGenerator.showAlert(AlertType.WARNING,
			                        Localization.getString(Strings.TITLE_WARNING),
			                        "", 
			                        Localization.getString(Strings.WARNING_NAME_CHARACTER_LIMIT_REACHED_150), 
			                        controller.getIcon(), 
			                        controller.getStage(), 
			                        null, 
			                        false);
			return;
		}

		String amountText = textFieldAmount.getText();
		if(!amountText.matches("^-?\\d+(,\\d+)*(\\.\\d+(e\\d+)?)?$"))
		{
			AlertGenerator.showAlert(AlertType.WARNING, 
			                        Localization.getString(Strings.TITLE_WARNING),
			                        "",
			                        Localization.getString(Strings.WARNING_PAYMENT_AMOUNT),
			                        controller.getIcon(),
			                        controller.getStage(),
			                        null,
			                        false);
			return;
		}

		LocalDate date = datePicker.getValue();
		if(date == null)
		{
			AlertGenerator.showAlert(AlertType.WARNING,
                			        Localization.getString(Strings.TITLE_WARNING),
                			        "",
                			        Localization.getString(Strings.WARNING_EMPTY_PAYMENT_DATE),
                			        controller.getIcon(),
                			        controller.getStage(),
                			        null,
                			        false);
			return;
		}

		int amount = 0;
		amount = (int)(Double.parseDouble(amountText.replace(",", ".")) * 100);
		if(isPayment)
		{
			amount = -amount;
		}
		
		String description = textArea.getText();
		if(description != null)
		{
			if(description.length() > 150)
			{
				AlertGenerator.showAlert(AlertType.WARNING,
				                        Localization.getString(Strings.TITLE_WARNING),
				                        "",
				                        Localization.getString(Strings.WARNING_DESCRIPTION_CHARACTER_LIMIT_REACHED_150),
				                        controller.getIcon(),
				                        controller.getStage(),
				                        null,
				                        false);
				return;
			}
		}
		else
		{
			description = "";
		}

		int repeatingInterval = 0;
		int repeatingDay = 0;
		if(checkBoxRepeat.isSelected())
		{
			if(radioButtonPeriod.isSelected())
			{
				repeatingInterval = spinnerRepeatingPeriod.getValue();
			}
			else
			{
				repeatingDay = comboBoxRepeatingDay.getValue();
			}

			if(repeatingInterval == 0 && repeatingDay == 0)
			{
				AlertGenerator.showAlert(AlertType.WARNING,
				        Localization.getString(Strings.TITLE_WARNING),
				        "",
				        Localization.getString(Strings.WARNING_PAYMENT_REPEATING),				       
				        controller.getIcon(),
				        controller.getStage(),
				        null,
				        false);
				return;
			}

			if(datePickerEnddate.getValue() != null && datePickerEnddate.getValue().isBefore(date))
			{
				AlertGenerator.showAlert(AlertType.WARNING, 
				                        Localization.getString(Strings.TITLE_WARNING),
				                        "",
				                        Localization.getString(Strings.WARNING_ENDDATE_BEFORE_STARTDATE),
				                        controller.getIcon(),
				                        controller.getStage(),
				                        null,
				                        false);
				return;
			}			

			if(edit)
			{				
				try
				{		
					RepeatingPayment newPayment = new RepeatingPayment(-1, amount, Helpers.getDateString(date), comboBoxCategory.getValue().getID(), name, description, repeatingInterval, Helpers.getDateString(datePickerEnddate.getValue()), repeatingDay);
							
					ServerConnection connection = new ServerConnection(controller.getSettings());
					if(payment instanceof NormalPayment)
					{
						connection.deleteNormalPayment((NormalPayment)payment);
					}
					else
					{	
						connection.deleteRepeatingPayment((RepeatingPaymentEntry)payment);						
					}	
					connection.addRepeatingPayment(newPayment);
				}
				catch(Exception e)
				{
					Logger.error(e);
					controller.showConnectionErrorAlert(ExceptionHandler.getMessageForException(e));
				}
			}
			else
			{
				RepeatingPayment newPayment = new RepeatingPayment(-1, amount, Helpers.getDateString(date), comboBoxCategory.getValue().getID(), name, description, repeatingInterval,Helpers.getDateString(datePickerEnddate.getValue()), repeatingDay);
				try
				{
					ServerConnection connection = new ServerConnection(controller.getSettings());
					connection.addRepeatingPayment(newPayment);
				}
				catch(Exception e)
				{
					Logger.error(e);
					controller.showConnectionErrorAlert(e.getMessage());
				}
			}
		}
		else
		{
			if(edit)
			{
				NormalPayment newPayment = new NormalPayment(payment.getID(), amount, Helpers.getDateString(date), comboBoxCategory.getValue().getID(), name, description);
				try
				{
					ServerConnection connection = new ServerConnection(controller.getSettings());
					if(payment instanceof RepeatingPaymentEntry)
					{
						//if old one was repeating it should be deleted
						connection.deleteRepeatingPayment((RepeatingPaymentEntry)payment);
						connection.addNormalPayment(newPayment);
					}
					else
					{
						connection.updateNormalPayment(newPayment);
					}					
				}
				catch(Exception e)
				{
					Logger.error(e);
					controller.showConnectionErrorAlert(e.getMessage());
				}
			}
			else
			{
				NormalPayment newPayment = new NormalPayment(-1, amount, Helpers.getDateString(date), comboBoxCategory.getValue().getID(), name, description);
				try
				{
					ServerConnection connection = new ServerConnection(controller.getSettings());
					connection.addNormalPayment(newPayment);
				}
				catch(Exception e)
				{
					Logger.error(e);
					controller.showConnectionErrorAlert(e.getMessage());
				}
			}
		}

		getStage().close();
		paymentController.getController().refresh(controller.getFilterSettings());
	}

	public void cancel()
	{
		getStage().close();
	}

	private void toggleRepeatingArea(boolean selected)
	{
		if(selected)
		{
			if(radioButtonPeriod.isSelected())
			{
				spinnerRepeatingPeriod.setDisable(false);
				comboBoxRepeatingDay.setDisable(true);
			}
			else
			{
				spinnerRepeatingPeriod.setDisable(true);
				comboBoxRepeatingDay.setDisable(false);
			}
		}
		else
		{
			spinnerRepeatingPeriod.setDisable(!selected);
			comboBoxRepeatingDay.setDisable(!selected);
		}
		datePickerEnddate.setDisable(!selected);
		radioButtonPeriod.setDisable(!selected);
		radioButtonDay.setDisable(!selected);
		labelText1.setDisable(!selected);
		labelText2.setDisable(!selected);
		labelText3.setDisable(!selected);
	}

	private void toggleRadioButtonPeriod(boolean selected)
	{
		spinnerRepeatingPeriod.setDisable(!selected);
		labelText1.setDisable(!selected);
		labelText2.setDisable(!selected);
		comboBoxRepeatingDay.setDisable(selected);
		labelText3.setDisable(selected);
	}
	
	@Override
	public void applyStyle()
	{
		buttonCancel.setGraphic(Helpers.getFontIcon(FontIconType.TIMES, 17, Color.WHITE));
		buttonSave.setGraphic(Helpers.getFontIcon(FontIconType.SAVE, 17, Color.WHITE));

		scrollPane.setStyle("-fx-background-color: transparent");
		buttonCancel.setStyle("-fx-background-color: " + ConvertTo.toRGBHexWithoutOpacity(Colors.BACKGROUND_BUTTON_BLUE) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
		buttonSave.setStyle("-fx-background-color: " + ConvertTo.toRGBHexWithoutOpacity(Colors.BACKGROUND_BUTTON_BLUE) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
	}
}