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
public class WhiteListNotFoundException extends TinyException {
	private static final long serialVersionUID = 42L;

	public WhiteListNotFoundException(final String msg) {
		super(msg);
	}
}