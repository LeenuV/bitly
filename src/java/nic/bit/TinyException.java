/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nic.bit;

/**
 *
 * @author nitin
 */
import java.net.MalformedURLException;

public class TinyException extends MalformedURLException {
	private static final long serialVersionUID = 42L;

	public TinyException(final String msg) {
		super(msg);
	}

	/**
	 * Speedup creation ignoring fillIn of stack trace
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
