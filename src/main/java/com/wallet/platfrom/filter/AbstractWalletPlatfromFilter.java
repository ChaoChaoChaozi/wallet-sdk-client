package com.wallet.platfrom.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wallet.platfrom.sdk.IWalletPlatfromInterface;
import com.wallet.platfrom.sdk.ProcessResult;
import com.wallet.platfrom.sdk.beans.OutputBean;
import com.wallet.platfrom.sdk.beans.WithdrawDataBean;
import com.wallet.platfrom.util.RSAResult;
import com.wallet.platfrom.util.RSAUtil;

public abstract class AbstractWalletPlatfromFilter implements Filter {

	private static String charset = null;
	
	public abstract IWalletPlatfromInterface getWalletPlatfromInterface();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		charset = filterConfig.getInitParameter("charset");
		if (null == charset || charset.trim().length() == 0) {
			charset = null;
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		request.setCharacterEncoding("UTF-8");
		String walletMethod = request.getParameter("wallet.platfrom.method");
		String data = request.getParameter("wallet.platfrom.data");
		String sign = request.getParameter("sign");
		
		startFilter(walletMethod, data, sign);
		
		if (null != walletMethod && walletMethod.trim().length() > 0) {
			if ("newaddress".equals(walletMethod)) {
				processNewAddress((HttpServletRequest) request, (HttpServletResponse) response, data, sign);
			} else if ("charge".equals(walletMethod)) {
				processChange((HttpServletRequest) request, (HttpServletResponse) response, data, sign);
			} else if ("confirm".equals(walletMethod)) {
				processConfirm((HttpServletRequest) request, (HttpServletResponse) response, data, sign);
			} else if ("getwithdrawdata".equals(walletMethod)) {
				processGetWithdrawData((HttpServletRequest) request, (HttpServletResponse) response, data, sign);
			} else if ("withdrawdataresult".equals(walletMethod)) {
				processWithdrawDataResult((HttpServletRequest) request, (HttpServletResponse) response, data, sign);
			} else {
				throw new ServletException("unknown wallet.platfrom.method <== " + walletMethod);
			}
		} else {
			chain.doFilter(request, response);
		}
	}
	
	protected void startFilter(String walletMethod, String data, String sign) {
		
	}

	@Override
	public void destroy() {
		
	}
	
	private void processNewAddress(HttpServletRequest request, HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		Map<String, String> paramMap = checkData(response, data, sign);
		if (null != paramMap) {
			String symbol = paramMap.get("symbol");
			if (null == symbol || symbol.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[symbol]为空");
				writeOutput(response, output);
				return;
			}
			
			String addresses = paramMap.get("addresses");
			List<String> addressList = new ArrayList<String>();
			if (null != addresses && addresses.trim().length() > 0) {
				addressList = Arrays.asList(addresses.split(","));
			}
			Integer numbers = getWalletPlatfromInterface().processAddress(symbol, addressList);
			numbers = (null == numbers) ? 0 : numbers;
			output.setSuccess(true);
			output.setCode(0);
			output.setMessage(String.valueOf(numbers.intValue()));
			writeOutput(response, output);
			return;
		}
	}

	private void processChange(HttpServletRequest request, HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		Map<String, String> paramMap = checkData(response, data, sign);
		if (null != paramMap) {
			String address = paramMap.get("address");
			if (null == address || address.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[address]为空");
				writeOutput(response, output);
				return;
			}
			
			String txid = paramMap.get("txid");
			if (null == txid || txid.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[txid]为空");
				writeOutput(response, output);
				return;
			}
			
			String amountStr = paramMap.get("amount");
			if (null == amountStr || amountStr.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[amount]为空");
				writeOutput(response, output);
				return;
			}
			
			BigDecimal amount = null;
			try {
				amount = new BigDecimal(amountStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (null == amount) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[amount]错误，无法转换为BigDecimal类型");
				writeOutput(response, output);
				return;
			}
			
			ProcessResult result = getWalletPlatfromInterface().charge(txid, address, amount);
			if (null == result) {
				output.setSuccess(true);
				output.setCode(0);
				output.setMessage("返回值为空，默认成功");
			} else {
				output.setSuccess(null == result.getSuccess() ? false : result.getSuccess());
				output.setCode(output.getSuccess() ? 0 : 1);
				output.setMessage(result.getMessage());
			}
			writeOutput(response, output);
			return;
		}
	}

	private void processConfirm(HttpServletRequest request, HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		Map<String, String> paramMap = checkData(response, data, sign);
		if (null != paramMap) {
			
			String txid = paramMap.get("txid");
			if (null == txid || txid.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[txid]为空");
				writeOutput(response, output);
				return;
			}
			
			String confirmsStr = paramMap.get("confirms");
			if (null == confirmsStr || confirmsStr.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[confirms]为空");
				writeOutput(response, output);
				return;
			}
			
			Integer confirms = null;
			try {
				confirms = Integer.parseInt(confirmsStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (null == confirms) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[confirms]错误，无法转换为Integer类型");
				writeOutput(response, output);
				return;
			}
			
			ProcessResult result = getWalletPlatfromInterface().confirmCharge(txid, confirms);
			
			if (null == result) {
				output.setSuccess(true);
				output.setCode(0);
				output.setMessage("返回值为空，默认成功");
			} else {
				output.setSuccess(null == result.getSuccess() ? false : result.getSuccess());
				output.setCode(output.getSuccess() ? 0 : 1);
				output.setMessage(result.getMessage());
			}
			writeOutput(response, output);
			return;
		}
	}

	private void processGetWithdrawData(HttpServletRequest request, HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		Map<String, String> paramMap = checkData(response, data, sign);
		if (null != paramMap) {
			List<WithdrawDataBean> datas = getWalletPlatfromInterface().getWithdrawDatas();
			output.setSuccess(true);
			output.setCode(0);
			output.setMessage(toJsonString(datas));
			writeOutput(response, output);
			return;
		}
	}
	
	private String toJsonString(List<WithdrawDataBean> datas) {
		if (null == datas || datas.size() == 0) {
			return "[]";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (int i = 0; i < datas.size(); ++i) {
			if (i > 0) {
				buf.append(",");
			}
			buf.append(datas.get(i).toString());
		}
		buf.append("]");
		
		return buf.toString();
	}

	private void processWithdrawDataResult(HttpServletRequest request, HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		Map<String, String> paramMap = checkData(response, data, sign);
		if (null != paramMap) {
			
			String successStr = paramMap.get("success");
			if (null == successStr || successStr.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[success]为空");
				writeOutput(response, output);
				return;
			}
			
			Boolean success = null;
			try {
				success = Boolean.valueOf(successStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (null == success) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[success]错误，无法解析为Boolean类型");
				writeOutput(response, output);
				return;
			}
			
			String txid = paramMap.get("txid");
			if (null == txid || txid.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[txid]为空");
				writeOutput(response, output);
				return;
			}
			
			String message = paramMap.get("message");
			
			String serno = paramMap.get("serno");
			if (null == serno || serno.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[serno]为空");
				writeOutput(response, output);
				return;
			}

			String symbol = paramMap.get("symbol");
			if (null == symbol || symbol.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[symbol]为空");
				writeOutput(response, output);
				return;
			}

			String address = paramMap.get("address");
			if (null == address || address.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[address]为空");
				writeOutput(response, output);
				return;
			}
			
			String amountStr = paramMap.get("amount");
			if (null == amountStr || amountStr.trim().length() == 0) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[amount]为空");
				writeOutput(response, output);
				return;
			}
			
			BigDecimal amount = null;
			try {
				amount = new BigDecimal(amountStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (null == amount) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage("参数[amount]错误，无法转换为BigDecimal类型");
				writeOutput(response, output);
				return;
			}
			
			WithdrawDataBean withdrawData = new WithdrawDataBean();
			withdrawData.setSerno(serno);
			withdrawData.setAmount(amount);
			withdrawData.setAddress(address);
			withdrawData.setSymbol(symbol);
			
			ProcessResult result = getWalletPlatfromInterface().processWithdrawResult(success, txid, message, withdrawData);
			
			if (null == result) {
				output.setSuccess(true);
				output.setCode(0);
				output.setMessage("返回值为空，默认成功");
			} else {
				output.setSuccess(null == result.getSuccess() ? false : result.getSuccess());
				output.setCode(output.getSuccess() ? 0 : 1);
				output.setMessage(result.getMessage());
			}
			writeOutput(response, output);
			return;
		}
	}
	
	private Map<String, String> checkData(HttpServletResponse response, String data, String sign) {
		OutputBean output = new OutputBean();
		RSAResult result = null;
		try {
			result = decrypt(data, sign);
		} catch (Exception e) {
			output.setSuccess(false);
			output.setCode(0);
			output.setMessage("解析数据异常，详情：" + e.getMessage());
			writeOutput(response, output);
			return null;
		}
		if (null == result) {
			output.setSuccess(false);
			output.setCode(0);
			output.setMessage("数据解析为空");
			writeOutput(response, output);
			return null;
		}
		
		if (!result.isSignRight()) {
			output.setSuccess(false);
			output.setCode(0);
			output.setMessage("数据签名错误");
			writeOutput(response, output);
			return null;
		}
		
		if (null == result.getData() || result.getData().trim().length() == 0) {
			output.setSuccess(false);
			output.setCode(0);
			output.setMessage("数据为空");
			writeOutput(response, output);
			return null;
		}
		
		return praseData(result.getData());
	}

	private void writeOutput(HttpServletResponse response, OutputBean output) {
		PrintWriter pw = null;
		try {
			response.reset();
			response.setHeader("Content-type", "text/html;charset=UTF-8");
			pw = response.getWriter();
			RSAResult result = null;
			String message = null;
			try {
				result = encrypt(output.toString());
			} catch (Exception e) {
				e.printStackTrace();
				message = e.getMessage();
			}
			if (null == result) {
				output.setSuccess(false);
				output.setCode(0);
				output.setMessage(message);
			} else {
				output.setMessage("");
				output.setSuccess(true);
				output.setCode(0);
				output.setData(result.getData());
				output.setSign(result.getSign());
			}
			String data = output.toString();
			writeData(data);
			pw.write(data);
			pw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != pw) {
				try {
					pw.close();
				} catch (Exception e) {
				}
				pw = null;
			}
		}
	}
	
	protected void writeData(String data) {
		
	}

	private RSAResult decrypt(String data, String sign) throws Exception {
		return RSAUtil.decript(getWalletPlatfromInterface().getConfig().getPublicKeyFile(), data, sign, charset);
	}

	private RSAResult encrypt(String data) throws Exception {
		return RSAUtil.encript(getWalletPlatfromInterface().getConfig().getPrivateKeyFile(), data, charset);
	}

	private static Map<String, String> praseData(String data) {
		Map<String, String> map = new HashMap<String, String>();
		if (null != data && data.trim().length() > 0) {
			String[] params = data.split("&");
			String[] param = null;
			for (int i = 0; i < params.length; ++i) {
				param = params[i].split("=");
				if (param.length >= 2) {
					map.put(param[0], param[1]);
				} else if (param.length == 1) {
					map.put(param[0], null);
				}
			}
		}
		return map;
	}
}
