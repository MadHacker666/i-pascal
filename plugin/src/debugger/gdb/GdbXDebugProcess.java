package com.siberika.idea.pascal.debugger.gdb;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.siberika.idea.pascal.PascalBundle;
import com.siberika.idea.pascal.debugger.PascalXDebugProcess;
import com.siberika.idea.pascal.jps.sdk.PascalSdkData;

import java.util.HashMap;

/**
 * Author: George Bakhtadze
 * Date: 26/03/2017
 */
public class GdbXDebugProcess extends PascalXDebugProcess {

    private static final Logger LOG = Logger.getInstance(GdbXDebugProcess.class);

    public GdbXDebugProcess(XDebugSession session, ExecutionEnvironment environment, ExecutionResult executionResult) {
        super(session, environment, executionResult);
    }

    @Override
    protected void init(ExecutionEnvironment environment) {
        try {
            createGdbProcess(environment);
        } catch (ExecutionException e) {
            LOG.warn("Error running GDB", e);
        }
    }

    @Override
    protected void doCreateTabLayouter(RunnerLayoutUi ui) {
        if (!isOutputConsoleNeeded()) {
            return;
        }
        Content gdbConsoleContent = ui.createContent("PascalDebugConsoleContent", outputConsole.getComponent(),
                PascalBundle.message("debug.output.title"), AllIcons.Debugger.Console, outputConsole.getPreferredFocusableComponent());
        gdbConsoleContent.setCloseable(false);

        DefaultActionGroup consoleActions = new DefaultActionGroup();
        AnAction[] actions = outputConsole.getConsole() != null ? outputConsole.getConsole().createConsoleActions() : EMPTY_ACTIONS;
        for (AnAction action : actions) {
            consoleActions.add(action);
        }
        gdbConsoleContent.setActions(consoleActions, ActionPlaces.DEBUGGER_TOOLBAR, outputConsole.getPreferredFocusableComponent());

        ui.addContent(gdbConsoleContent, 2, PlaceInGrid.bottom, false);
    }

    private void createGdbProcess(ExecutionEnvironment env) throws ExecutionException {
        if (isOutputConsoleNeeded()) {
            createOutputConsole(env.getProject());
        }
        console = (ConsoleView) executionResult.getExecutionConsole();
        variableObjectMap = new HashMap<String, GdbVariableObject>();
        sendCommand("-break-delete");
    }

    @Override
    public void sessionInitialized() {
        super.sessionInitialized();
        getProcessHandler().addProcessListener(new GdbProcessAdapter(this));
        sendCommand("-gdb-set target-async on");
        if (getData().getBoolean(PascalSdkData.Keys.DEBUGGER_REDIRECT_CONSOLE)) {
            if (SystemInfo.isWindows) {
                sendCommand("-gdb-set new-console on");
            } else {
                sendCommand("-exec-arguments > " + outputFile.getAbsolutePath());
            }
        } 
        sendCommand("-exec-run");
        getSession().setPauseActionSupported(true);
    }

}
