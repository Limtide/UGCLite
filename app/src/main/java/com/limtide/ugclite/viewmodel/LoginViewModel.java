package com.limtide.ugclite.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.limtide.ugclite.database.entity.User;
import com.limtide.ugclite.data.repository.UserRepository;
import com.limtide.ugclite.data.repository.UserRepository.LoginResult;
import com.limtide.ugclite.utils.MD5Utils;

/**
 * 登录页面的ViewModel
 * 处理登录相关的业务逻辑和状态管理
 */
public class LoginViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    // 登录表单数据
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> password = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rememberPassword = new MutableLiveData<>(false);

    // 登录状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    // 表单验证状态
    private final MutableLiveData<Boolean> isUsernameValid = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isPasswordValid = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isLoginFormValid = new MutableLiveData<>(false);

    public LoginViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);

        // 观察表单数据变化，更新登录按钮状态
        username.observeForever(username -> validateLoginForm());
        password.observeForever(password -> validateLoginForm());

        // 创建测试用户数据
        createTestUsers();
    }

    /**
     * 设置用户名
     */
    public void setUsername(String value) {
        username.setValue(value);
        validateUsername(value);
    }

    /**
     * 设置密码
     */
    public void setPassword(String value) {
        password.setValue(value);
        validatePassword(value);
    }

    /**
     * 设置记住密码状态
     */
    public void setRememberPassword(boolean remember) {
        rememberPassword.setValue(remember);
    }

    // LiveData getters
    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getPassword() {
        return password;
    }

    public LiveData<Boolean> getRememberPassword() {
        return rememberPassword;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public void setIsLoading(boolean isloading) {
      isLoading.setValue(isloading);
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsUsernameValid() {
        return isUsernameValid;
    }

    public LiveData<Boolean> getIsPasswordValid() {
        return isPasswordValid;
    }

    public LiveData<Boolean> getIsLoginFormValid() {
        return isLoginFormValid;
    }

    /**
     * 验证用户名
     */
    private void validateUsername(String username) {
        boolean isValid = !TextUtils.isEmpty(username.trim());
        isUsernameValid.setValue(isValid);
        if (!isValid) {
            errorMessage.setValue("请输入用户名");
        } else {
            clearErrorMessage();
        }
    }

    /**
     * 验证密码
     */
    private void validatePassword(String password) {
        boolean isValid = !TextUtils.isEmpty(password.trim()) && password.length() >= 6;
        isPasswordValid.setValue(isValid);
        if (!isValid) {
            if (TextUtils.isEmpty(password.trim())) {
                errorMessage.setValue("请输入密码");
            } else {
                errorMessage.setValue("密码长度至少6位");
            }
        } else {
            clearErrorMessage();
        }
    }

    /**
     * 验证登录表单
     */
    private void validateLoginForm() {
        String usernameValue = username.getValue();
        String passwordValue = password.getValue();

        boolean isValid = usernameValue != null && !usernameValue.trim().isEmpty() &&
                passwordValue != null && !passwordValue.trim().isEmpty() &&
                passwordValue.length() >= 6;

        isLoginFormValid.setValue(isValid);
    }

    /**
     * 执行登录
     */
    public void login() {
        String usernameValue = username.getValue();
        String passwordValue = password.getValue();

        if (usernameValue == null || passwordValue == null) {
            errorMessage.setValue("请填写完整的登录信息");
            return;
        }

        // 重新验证表单
        validateUsername(usernameValue);
        validatePassword(passwordValue);

        Boolean isFormValid = isLoginFormValid.getValue();
        if (isFormValid == null || !isFormValid) {
            return;
        }

        // 显示加载状态
        isLoading.setValue(true);
        clearErrorMessage();

        String password =MD5Utils.encrypt(passwordValue);

        // 执行登录
        userRepository.loginUser(usernameValue.trim(), password);

//        // 观察登录结果
//        observeForever会导致内存泄漏，同时在viewmodel观察不符合MVVM模式
//        userRepository.getLoginResult().observeForever(loginResult -> {
//            isLoading.setValue(false);
//            if (loginResult != null) {
//                if (loginResult.isSuccess()) {
//                    successMessage.setValue(loginResult.getMessage());
//                    // 这里可以添加登录成功后的跳转逻辑
//                } else {
//                    errorMessage.setValue(loginResult.getMessage());
//                }
//            }
//        });
    }


    public void loginWithWeChat() {
        // 这里只显示Toast提示，不实现真实的微信登录
        successMessage.setValue("微信登录功能开发中，敬请期待");
    }


    public void loginWithApple() {
        // 这里只显示Toast提示，不实现真实的Apple登录
        successMessage.setValue("Apple登录功能开发中，敬请期待");
    }


    public void onForgotPassword() {
        successMessage.setValue("请联系管理员重置密码");
    }


    public void onRegisterClick() {
        successMessage.setValue("注册功能开发中，敬请期待");
    }

    /**
     * 创建测试用户数据
     */
    public void createTestUsers() {
        userRepository.createTestUsers();
    }

    /**
     * 清除错误消息
     */
    private void clearErrorMessage() {
        if (errorMessage.getValue() != null) {
            errorMessage.setValue(null);
        }
    }

    /**
     * 清除成功消息
     */
    private void clearSuccessMessage() {
        if (successMessage.getValue() != null) {
            successMessage.setValue(null);
        }
    }

    /**
     * 清除所有消息
     */
    public void clearAllMessages() {
        clearErrorMessage();
        clearSuccessMessage();
    }

    /**
     * 获取当前用户
     */
    public LiveData<User> getCurrentUser(String username) {
        return userRepository.getUserByUsername(username);
    }
    public LiveData<LoginResult> getLoginResult() {
        return userRepository.getLoginResult();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.cleanup();
    }
}