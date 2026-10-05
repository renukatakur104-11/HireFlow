import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import toast from 'react-hot-toast';
import { Briefcase, Mail, Lock, ArrowRight } from 'lucide-react';
import { loginSchema, LoginFormValues } from '../../lib/validation/authSchemas';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../lib/api/client';
import { API } from '../../lib/api/endpoints';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card } from '../../components/ui/Card';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || null;

  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginFormValues) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post(API.auth.login, data);
      const { accessToken, refreshToken, user } = res.data.data;
      login(accessToken, refreshToken, user);
      toast.success(`Welcome back, ${user.fullName || user.email}!`);
      navigate(from || (user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'), { replace: true });
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally { setIsLoading(false); }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 shadow-xl shadow-indigo-500/30 mb-4">
            <Briefcase className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-white">Welcome back</h1>
          <p className="mt-1 text-slate-400 text-sm">Sign in to your HireFlow account</p>
        </div>
        <Card className="border-slate-700/50 bg-slate-800/50 backdrop-blur-sm">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input label="Email Address" type="email" placeholder="you@college.edu"
              leftIcon={<Mail className="w-4 h-4" />} error={errors.email?.message}
              className="bg-slate-700/50 border-slate-600 text-white placeholder:text-slate-400"
              {...register('email')} />
            <Input label="Password" type="password" placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />} error={errors.password?.message}
              className="bg-slate-700/50 border-slate-600 text-white placeholder:text-slate-400"
              {...register('password')} />
            <Button type="submit" isLoading={isLoading} className="w-full" rightIcon={<ArrowRight className="w-4 h-4" />}>
              Sign In
            </Button>
          </form>
        </Card>
        <p className="text-center text-slate-400 text-sm">
          New student? <Link to="/register" className="text-indigo-400 hover:text-indigo-300 font-medium">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
