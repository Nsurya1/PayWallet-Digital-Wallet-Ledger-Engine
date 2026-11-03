import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  User,
  KeyRound,
  Lock,
  Eye,
  EyeOff,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  CheckCircle2,
  Copy,
  Check,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { AuthSplitLayout } from '@/components/auth/AuthSplitLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { PasswordStrengthMeter } from '@/components/auth/PasswordStrengthMeter';
import { authApi } from '@/api/authApi';
import { ForgotPasswordResponse } from '@/types';

// Step 1 schema: request reset code
const requestSchema = z.object({
  identifier: z.string().min(1, 'Username or email is required').trim(),
});

type RequestFormData = z.infer<typeof requestSchema>;

// Step 2 schema: reset password
const resetSchema = z
  .object({
    resetCode: z.string().min(6, 'Reset code must be 6 digits').max(64).trim(),
    newPassword: z.string().min(8, 'New password must be at least 8 characters long'),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type ResetFormData = z.infer<typeof resetSchema>;

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<1 | 2>(1);
  const [resetData, setResetData] = useState<ForgotPasswordResponse | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Form for Step 1
  const {
    register: registerRequest,
    handleSubmit: handleRequestSubmit,
    setValue: setRequestValue,
    formState: { errors: requestErrors, isSubmitting: isRequesting },
  } = useForm<RequestFormData>({
    resolver: zodResolver(requestSchema),
    defaultValues: {
      identifier: '',
    },
  });

  // Form for Step 2
  const {
    register: registerReset,
    handleSubmit: handleResetSubmit,
    setValue: setResetValue,
    watch: watchReset,
    formState: { errors: resetErrors, isSubmitting: isResetting },
  } = useForm<ResetFormData>({
    resolver: zodResolver(resetSchema),
    defaultValues: {
      resetCode: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const watchedNewPassword = watchReset('newPassword');

  // Submit Step 1: Request Code
  const onRequestCode = async (data: RequestFormData) => {
    setServerError(null);
    try {
      const response = await authApi.forgotPassword(data.identifier);
      setResetData(response);
      setResetValue('resetCode', response.resetCode);
      setStep(2);
      toast.success('Reset code generated successfully!');
    } catch (err: unknown) {
      let message = 'Failed to generate reset code. Please verify your username or email.';
      if (err && typeof err === 'object' && 'response' in err) {
        const responseData = (err as { response?: { data?: { message?: string } } }).response?.data;
        if (responseData?.message) {
          message = responseData.message;
        }
      }
      setServerError(message);
      toast.error(message);
    }
  };

  // Submit Step 2: Reset Password
  const onResetPassword = async (data: ResetFormData) => {
    if (!resetData) return;
    setServerError(null);
    try {
      await authApi.resetPassword({
        identifier: resetData.identifier,
        resetCode: data.resetCode,
        newPassword: data.newPassword,
      });
      toast.success('Password reset successfully! Please sign in with your new password.');
      navigate('/login', { replace: true });
    } catch (err: unknown) {
      let message = 'Failed to reset password. Please verify the reset code.';
      if (err && typeof err === 'object' && 'response' in err) {
        const responseData = (err as { response?: { data?: { message?: string } } }).response?.data;
        if (responseData?.message) {
          message = responseData.message;
        }
      }
      setServerError(message);
      toast.error(message);
    }
  };

  const handleCopyCode = () => {
    if (!resetData?.resetCode) return;
    navigator.clipboard.writeText(resetData.resetCode);
    setCopiedCode(true);
    toast.success('Reset code copied to clipboard');
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const handleFillDemo = () => {
    setRequestValue('identifier', 'rahul', { shouldValidate: true });
    toast.info('Filled demo account (rahul)');
  };

  return (
    <AuthSplitLayout
      heading={step === 1 ? 'Reset your password' : 'Create new password'}
      subheading={
        step === 1
          ? 'Enter your registered username or email to receive a secure recovery code.'
          : 'Enter your 6-digit recovery code and choose a strong new password.'
      }
    >
      {/* Progress Steps Indicator */}
      <div className="mb-6 flex items-center justify-between border-b border-border pb-4">
        <div className="flex items-center gap-3">
          <div
            className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
              step >= 1
                ? 'bg-primary text-primary-foreground shadow-sm'
                : 'bg-muted text-muted-foreground'
            }`}
          >
            1
          </div>
          <span className={`text-xs font-semibold ${step >= 1 ? 'text-foreground' : 'text-muted-foreground'}`}>
            Verify Account
          </span>
        </div>
        <div className="w-8 h-[2px] bg-border mx-2" />
        <div className="flex items-center gap-3">
          <div
            className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
              step === 2
                ? 'bg-primary text-primary-foreground shadow-sm'
                : 'bg-muted text-muted-foreground'
            }`}
          >
            2
          </div>
          <span className={`text-xs font-semibold ${step === 2 ? 'text-foreground' : 'text-muted-foreground'}`}>
            Set New Password
          </span>
        </div>
      </div>

      {/* Server Error Alert */}
      {serverError && (
        <div className="mb-5 p-3.5 rounded-xl bg-danger/15 border border-danger/30 text-danger text-xs flex items-center gap-2.5 animate-fade-in">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{serverError}</span>
        </div>
      )}

      {/* STEP 1: REQUEST CODE */}
      {step === 1 && (
        <form onSubmit={handleRequestSubmit(onRequestCode)} className="space-y-4" noValidate>
          <Input
            label="Username or Email"
            type="text"
            placeholder="e.g. rahul or rahul@paywallet.com"
            autoComplete="username"
            leftIcon={<User className="w-4 h-4" />}
            error={requestErrors.identifier?.message}
            {...registerRequest('identifier')}
          />

          <Button
            type="submit"
            variant="gradient"
            size="lg"
            isLoading={isRequesting}
            className="w-full mt-2"
          >
            <span>Send Reset Code</span>
            <ArrowRight className="w-4 h-4" />
          </Button>

          {/* Demo Account Quick-Fill Chip */}
          <div className="pt-2">
            <button
              type="button"
              onClick={handleFillDemo}
              className="w-full py-2.5 px-3 rounded-xl border border-dashed border-primary/40 bg-primary/5 hover:bg-primary/10 text-primary transition-all text-xs font-medium flex items-center justify-center gap-2 group"
            >
              <Sparkles className="w-3.5 h-3.5 text-primary group-hover:scale-110 transition-transform" />
              <span>Auto-fill Demo Account (<strong>rahul</strong>)</span>
            </button>
          </div>
        </form>
      )}

      {/* STEP 2: ENTER CODE & SET NEW PASSWORD */}
      {step === 2 && resetData && (
        <form onSubmit={handleResetSubmit(onResetPassword)} className="space-y-4" noValidate>
          {/* Recovery Code Banner */}
          <div className="p-4 rounded-2xl bg-primary/10 border border-primary/20 text-foreground space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-primary flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4" />
                Recovery Code Generated
              </span>
              <button
                type="button"
                onClick={handleCopyCode}
                className="text-xs text-primary hover:underline flex items-center gap-1 font-semibold"
              >
                {copiedCode ? <Check className="w-3.5 h-3.5 text-success" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedCode ? 'Copied' : 'Copy'}</span>
              </button>
            </div>
            <div className="flex items-baseline justify-between">
              <div className="text-2xl font-mono font-bold tracking-widest text-primary">
                {resetData.resetCode}
              </div>
              <span className="text-[11px] text-muted-foreground">
                Expires in {Math.round(resetData.expiresInSeconds / 60)} mins
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              Account: <strong className="text-foreground">{resetData.identifier}</strong> ({resetData.maskedEmail})
            </p>
          </div>

          {/* Reset Code Input */}
          <Input
            label="6-Digit Reset Code"
            type="text"
            maxLength={6}
            placeholder="e.g. 123456"
            leftIcon={<KeyRound className="w-4 h-4" />}
            error={resetErrors.resetCode?.message}
            {...registerReset('resetCode')}
          />

          {/* New Password Input */}
          <div>
            <Input
              label="New Password"
              type={showPassword ? 'text' : 'password'}
              placeholder="Enter new strong password"
              autoComplete="new-password"
              leftIcon={<Lock className="w-4 h-4" />}
              rightIcon={
                <button
                  type="button"
                  tabIndex={-1}
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="text-muted-foreground hover:text-foreground transition-colors p-1"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
              error={resetErrors.newPassword?.message}
              {...registerReset('newPassword')}
            />
            <PasswordStrengthMeter password={watchedNewPassword || ''} />
          </div>

          {/* Confirm Password Input */}
          <Input
            label="Confirm New Password"
            type={showConfirmPassword ? 'text' : 'password'}
            placeholder="Repeat new password"
            autoComplete="new-password"
            leftIcon={<Lock className="w-4 h-4" />}
            rightIcon={
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                className="text-muted-foreground hover:text-foreground transition-colors p-1"
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            }
            error={resetErrors.confirmPassword?.message}
            {...registerReset('confirmPassword')}
          />

          <Button
            type="submit"
            variant="gradient"
            size="lg"
            isLoading={isResetting}
            className="w-full mt-2"
          >
            <span>Update Password</span>
            <CheckCircle2 className="w-4 h-4" />
          </Button>

          {/* Step Back Action */}
          <button
            type="button"
            onClick={() => {
              setStep(1);
              setServerError(null);
            }}
            className="w-full py-2 text-xs text-muted-foreground hover:text-foreground transition-colors flex items-center justify-center gap-1.5"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Change username / email</span>
          </button>
        </form>
      )}

      {/* Return to Sign In Link */}
      <div className="mt-8 text-center text-xs text-muted-foreground">
        Remember your password?{' '}
        <NavLink to="/login" className="font-semibold text-primary hover:underline">
          Sign In
        </NavLink>
      </div>
    </AuthSplitLayout>
  );
}
