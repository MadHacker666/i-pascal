package com.siberika.idea.pascal.debugger;

import com.intellij.openapi.ui.MessageType;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.siberika.idea.pascal.PascalBundle;
import com.siberika.idea.pascal.debugger.gdb.parser.GdbMiResults;
import com.siberika.idea.pascal.jps.sdk.PascalSdkData;
import com.siberika.idea.pascal.jps.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: George Bakhtadze
 * Date: 26/03/2017
 */
public class PascalLineBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<PascalLineBreakpointProperties>> {
    private final PascalXDebugProcess debugProcess;
    final Map<PascalLineBreakpointProperties, Integer> breakIndexMap = new HashMap<PascalLineBreakpointProperties, Integer>();

    public PascalLineBreakpointHandler(PascalXDebugProcess debugProcess) {
        super(PascalLineBreakpointType.class);
        this.debugProcess = debugProcess;
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<PascalLineBreakpointProperties> breakpoint) {
        PascalLineBreakpointProperties props = breakpoint.getProperties();
        int line = breakpoint.getLine();
        String filename = breakpoint.getPresentableFilePath();
        if (props != null) {
            line = props.getLine();
            filename = props.getFilename();
        }
        if (!PascalXDebugProcess.getData(PascalXDebugProcess.retrieveSdk(debugProcess.environment)).getBoolean(PascalSdkData.Keys.DEBUGGER_BREAK_FULL_NAME)) {
            filename = FileUtil.getFilename(filename);
        }
        debugProcess.sendCommand(String.format("-break-insert %s -f %s:%d", debugProcess.isInferiorRunning() ? "-h" : "", filename, line + 1));
    }

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<PascalLineBreakpointProperties> breakpoint, boolean temporary) {
        PascalLineBreakpointProperties props = breakpoint.getProperties();
        props = props != null ? props : new PascalLineBreakpointProperties(breakpoint.getPresentableFilePath(), breakpoint.getLine());
        Integer ind = breakIndexMap.get(props);
        if (ind != null) {
            debugProcess.sendCommand(String.format("-break-delete %d", ind));
        } else {
            debugProcess.getSession().reportMessage(PascalBundle.message("debug.breakpoint.notFound"), MessageType.ERROR);
        }
    }

    public void handleBreakpointResult(GdbMiResults bp) {
        String fullname = bp.getString("fullname");
        Integer line = bp.getInteger("line");
        if (fullname != null && line != null) {
            breakIndexMap.put(new PascalLineBreakpointProperties(fullname, line), bp.getInteger("number"));
        }
    }
}
