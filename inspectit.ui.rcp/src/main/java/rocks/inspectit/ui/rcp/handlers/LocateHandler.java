package rocks.inspectit.ui.rcp.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import rocks.inspectit.shared.all.communication.data.ExceptionSensorData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.tracing.data.Span;
import rocks.inspectit.ui.rcp.editor.ISubView;
import rocks.inspectit.ui.rcp.editor.root.AbstractRootEditor;
import rocks.inspectit.ui.rcp.editor.tree.SteppingTreeSubView;

/**
 * Abstract handler for all other handlers that are working with locate functionality.
 *
 * @author Ivan Senic
 *
 */
public abstract class LocateHandler extends AbstractTemplateHandler {

	/**
	 * Command executed by this handler.
	 */
	public static final String COMMAND = "rocks.inspectit.ui.rcp.commands.addToSteppingObjects";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (activeEditor instanceof AbstractRootEditor) {
			AbstractRootEditor rootEditor = (AbstractRootEditor) activeEditor;
			ISubView mainView = rootEditor.getSubView();
			SteppingTreeSubView steppingTreeSubView = mainView.getSubView(SteppingTreeSubView.class);
			if (steppingTreeSubView != null) {
				StructuredSelection structuredSelection = (StructuredSelection) HandlerUtil.getCurrentSelection(event);
				List<Object> templates = this.getTemplates(structuredSelection);
				for (Object objectToLocate : templates) {
					steppingTreeSubView.addObjectToSteppingControl(objectToLocate);
				}

				// switch this to tree tab
				mainView.select(steppingTreeSubView);
			}
		}
		return null;
	}

	/**
	 * Return {@link AbstractTemplateDefinitionDialog}.
	 *
	 * @param structuredSelection
	 *            Current {@link StructuredSelection}.
	 *
	 * @return {@link AbstractTemplateDefinitionDialog}.
	 */
	public abstract List<Object> getTemplates(StructuredSelection structuredSelection);

	/**
	 * {@link LocateHandler} for
	 * {@link rocks.inspectit.shared.cs.communication.data.SqlStatementData}.
	 *
	 * @author Ivan Senic
	 *
	 */
	public static final class SqlLocateHandler extends LocateHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Object> getTemplates(StructuredSelection structuredSelection) {
			List<Object> results = new ArrayList<>();
			for (Object selected : structuredSelection.toList()) {
				if (selected instanceof SqlStatementData) {
					results.add(super.getTemplate((SqlStatementData) selected, true, true, true));
				}
			}
			return results;
		}

	}

	/**
	 * {@link LocateHandler} for
	 * {@link rocks.inspectit.shared.cs.communication.data.ExceptionSensorData}.
	 *
	 * @author Ivan Senic
	 *
	 */
	public static final class ExceptionLocateHandler extends LocateHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Object> getTemplates(StructuredSelection structuredSelection) {
			List<Object> results = new ArrayList<>();
			for (Object selected : structuredSelection.toList()) {
				if (selected instanceof ExceptionSensorData) {
					results.add(super.getTemplate((ExceptionSensorData) selected, true, true, true, true, true));
				}
			}
			return results;
		}

	}

	/**
	 * {@link LocateHandler} for
	 * {@link rocks.inspectit.shared.cs.communication.data.InvocationSequenceData}.
	 *
	 * @author Ivan Senic
	 *
	 */
	public static final class InvocationLocateHandler extends LocateHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Object> getTemplates(StructuredSelection structuredSelection) {
			List<Object> results = new ArrayList<>();
			for (Object selected : structuredSelection.toList()) {
				if (selected instanceof InvocationSequenceData) {
					InvocationSequenceData invocationSequenceData = (InvocationSequenceData) selected;

					if (null != invocationSequenceData.getSqlStatementData()) {
						// if we have SQL add it to list without parameters, cause we want to find
						// same in invocation tree
						results.add(super.getTemplate(invocationSequenceData.getSqlStatementData(), false, true, false));

						// add also a timer data or created timer data, so that we can find method
						if ((null != invocationSequenceData.getTimerData()) && TimerData.class.equals(invocationSequenceData.getTimerData().getClass())) {
							results.add(super.getTemplate(invocationSequenceData.getTimerData(), false, true));
						} else {
							results.add(super.getTemplate(getTimerDataForSql(invocationSequenceData.getSqlStatementData()), false, true));
						}
					} else if ((null != invocationSequenceData.getExceptionSensorDataObjects()) && !invocationSequenceData.getExceptionSensorDataObjects().isEmpty()) {
						// locate all exceptions of the same throwable type
						// we don't care here for stake trace, message and exception event
						ExceptionSensorData data = invocationSequenceData.getExceptionSensorDataObjects().get(0);
						results.add(super.getTemplate(data, false, true, false, false, false));
					} else if ((null != invocationSequenceData.getTimerData()) && TimerData.class.equals(invocationSequenceData.getTimerData().getClass())) {
						// if we have timer and not http timer data add
						results.add(super.getTemplate(invocationSequenceData.getTimerData(), false, true));
					} else {
						// at the end if we have nothing add the invocation itself
						results.add(super.getTemplate(invocationSequenceData, false, true));
					}
				}
			}
			return results;
		}

		/**
		 * Returns {@link TimerData} instance with the same method ident as given
		 * {@link SqlStatementData}.
		 *
		 * @param sqlStatementData
		 *            {@link SqlStatementData}.
		 * @return Returns {@link TimerData} instance with the same method ident as given
		 *         {@link SqlStatementData}.
		 */
		private TimerData getTimerDataForSql(SqlStatementData sqlStatementData) {
			TimerData timerData = new TimerData();
			timerData.setMethodIdent(sqlStatementData.getMethodIdent());
			return timerData;
		}

	}

	/**
	 * {@link LocateHandler} for {@link rocks.inspectit.shared.cs.communication.data.TimerData}.
	 *
	 * @author Ivan Senic
	 *
	 */
	public static final class TimerLocateHandler extends LocateHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Object> getTemplates(StructuredSelection structuredSelection) {
			List<Object> results = new ArrayList<>();
			for (Object selected : structuredSelection.toList()) {
				if (selected instanceof TimerData) {
					results.add(super.getTemplate((TimerData) selected, true, true));
				}
			}
			return results;
		}

	}

	/**
	 * {@link LocateHandler} for {@link rocks.inspectit.shared.all.tracing.data.Span}.
	 *
	 * @author Ivan Senic
	 *
	 */
	public static final class SpanLocateHandler extends LocateHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Object> getTemplates(StructuredSelection structuredSelection) {
			List<Object> results = new ArrayList<>();
			for (Object selected : structuredSelection.toList()) {
				if (selected instanceof Span) {
					results.add(selected);
				}
			}
			return results;
		}

	}

}
