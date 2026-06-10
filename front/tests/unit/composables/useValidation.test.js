import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useValidationRules } from '@/composables/useValidation'

describe('useValidation 组合式函数测试', () => {
  let rules

  beforeEach(() => {
    rules = useValidationRules()
  })

  // ========== account 规则测试 ==========
  describe('account 规则', () => {
    it('应该通过有效的用户名', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, 'test_user', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过有效的邮箱格式', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, 'test@example.com', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过有效的手机号格式', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, '13812345678', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝无效的邮箱格式', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, 'invalid-email@', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝无效的手机号格式', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, '12345678901', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('空值应该返回必填错误', () => {
      const rule = rules.account()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('必填')
    })
  })

  // ========== password 规则测试 ==========
  describe('password 规则', () => {
    it('应该通过有效的密码（>=6字符）', () => {
      const rule = rules.password()
      const callback = vi.fn()
      rule.validator({}, 'abcdef', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝长度不足的密码（<6字符）', () => {
      const rule = rules.password()
      const callback = vi.fn()
      rule.validator({}, 'abc', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('至少为6')
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.password()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过包含特殊字符的密码', () => {
      const rule = rules.password()
      const callback = vi.fn()
      rule.validator({}, 'pass@123', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过恰好6个字符的密码', () => {
      const rule = rules.password()
      const callback = vi.fn()
      rule.validator({}, '123456', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== strongPassword 规则测试 ==========
  describe('strongPassword 规则', () => {
    it('应该通过同时包含字母和数字的密码', () => {
      const rule = rules.strongPassword()
      const callback = vi.fn()
      rule.validator({}, 'abc123', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝只包含字母的密码', () => {
      const rule = rules.strongPassword()
      const callback = vi.fn()
      rule.validator({}, 'abcdef', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('字母和数字')
    })

    it('应该拒绝只包含数字的密码', () => {
      const rule = rules.strongPassword()
      const callback = vi.fn()
      rule.validator({}, '123456', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('字母和数字')
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.strongPassword()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== captcha 规则测试 ==========
  describe('captcha 规则', () => {
    it('应该通过4位验证码', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, '1234', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过6位验证码', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, '654321', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过5位验证码', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, 'abcde', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝3位及以下的验证码', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, '123', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('4-6')
    })

    it('应该拒绝7位及以上的验证码', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, '1234567', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.captcha()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== email 规则测试 ==========
  describe('email 规则', () => {
    it('应该通过标准邮箱格式', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, 'user@example.com', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过带点号的邮箱', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, 'first.last@domain.org', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝不含@的地址', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, 'invalidemail.com', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('邮箱')
    })

    it('应该拒绝不含域名的地址', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, 'user@', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝含空格的地址', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, 'user @example.com', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.email()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== phone 规则测试 ==========
  describe('phone 规则', () => {
    it('应该通过有效的11位手机号（13开头）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '13812345678', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过有效的11位手机号（19开头）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '19812345678', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过有效的11位手机号（17开头）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '17612345678', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝12开头的号码', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '12345678901', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('手机')
    })

    it('应该拒绝位数不对的号码（10位）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '1381234567', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝位数不对的号码（12位）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '138123456789', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝含字母的号码', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '138abcd5678', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.phone()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== required 规则测试 ==========
  describe('required 规则', () => {
    it('应该返回必填规则对象', () => {
      const rule = rules.required()
      expect(rule.required).toBe(true)
      expect(rule.trigger).toContain('blur')
      expect(rule.trigger).toContain('change')
    })

    it('应该使用默认错误消息', () => {
      const rule = rules.required()
      expect(rule.message).toContain('必填')
    })

    it('应该支持自定义错误消息', () => {
      const rule = rules.required('此项不能为空')
      expect(rule.message).toBe('此项不能为空')
    })
  })

  // ========== minLength 规则测试 ==========
  describe('minLength 规则', () => {
    it('应该返回包含min属性的规则对象', () => {
      const rule = rules.minLength(5)
      expect(rule.min).toBe(5)
    })

    it('应该使用默认错误消息', () => {
      const rule = rules.minLength(3)
      expect(rule.message).toContain('长度不足')
    })

    it('应该支持自定义错误消息', () => {
      const rule = rules.minLength(10, '至少输入10个字符')
      expect(rule.message).toBe('至少输入10个字符')
    })

    it('应该支持不同的最小长度值', () => {
      const rule1 = rules.minLength(1)
      const rule2 = rules.minLength(100)
      expect(rule1.min).toBe(1)
      expect(rule2.min).toBe(100)
    })
  })

  // ========== maxLength 规则测试 ==========
  describe('maxLength 规则', () => {
    it('应该返回包含max属性的规则对象', () => {
      const rule = rules.maxLength(20)
      expect(rule.max).toBe(20)
    })

    it('应该使用默认错误消息', () => {
      const rule = rules.maxLength(10)
      expect(rule.message).toContain('长度超出')
    })

    it('应该支持自定义错误消息', () => {
      const rule = rules.maxLength(50, '最多输入50个字符')
      expect(rule.message).toBe('最多输入50个字符')
    })
  })

  // ========== username 规则测试 ==========
  describe('username 规则', () => {
    it('应该通过有效的用户名（字母数字下划线，3-20位）', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'valid_user123', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过最小长度用户名（3位）', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'abc', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过最大长度用户名（20位）', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'a'.repeat(20), callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝太短的用户名（<3位）', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'ab', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('3-20')
    })

    it('应该拒绝太长的用户名（>20位）', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'a'.repeat(21), callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝含特殊字符的用户名', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'user@name', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('应该拒绝含空格的用户名', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, 'user name', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    })

    it('空值应该返回必填错误', () => {
      const rule = rules.username()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('必填')
    })
  })

  // ========== url 规则测试 ==========
  describe('url 规则', () => {
    it('应该通过有效的URL', () => {
      const rule = rules.url()
      const callback = vi.fn()
      rule.validator({}, 'https://example.com', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该通过带路径的URL', () => {
      const rule = rules.url()
      const callback = vi.fn()
      rule.validator({}, 'http://example.com/path?query=1', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('应该拒绝无效的URL', () => {
      const rule = rules.url()
      const callback = vi.fn()
      rule.validator({}, 'not-a-url', callback)
      expect(callback).toHaveBeenCalledTimes(1)
      expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
      expect(callback.mock.calls[0][0].message).toContain('URL')
    })

    it('空值应该直接通过（非必填）', () => {
      const rule = rules.url()
      const callback = vi.fn()
      rule.validator({}, '', callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })

  // ========== 自定义验证器组合测试 ==========
  describe('自定义验证器组合', () => {
    it('应该可以同时使用多个规则', () => {
      const { required, email, minLength } = useValidationRules()
      const formRules = {
        name: [required(), minLength(3)],
        contact: [required('联系方式不能为空'), email()]
      }

      expect(formRules.name).toHaveLength(2)
      expect(formRules.name[0].required).toBe(true)
      expect(formRules.name[1].min).toBe(3)
      expect(formRules.contact).toHaveLength(2)
      expect(formRules.contact[0].message).toBe('联系方式不能为空')
      expect(formRules.contact[1]).toHaveProperty('validator')
    })

    it('应该返回完整的规则集合', () => {
      expect(rules).toHaveProperty('required')
      expect(rules).toHaveProperty('username')
      expect(rules).toHaveProperty('email')
      expect(rules).toHaveProperty('phone')
      expect(rules).toHaveProperty('password')
      expect(rules).toHaveProperty('strongPassword')
      expect(rules).toHaveProperty('account')
      expect(rules).toHaveProperty('captcha')
      expect(rules).toHaveProperty('minLength')
      expect(rules).toHaveProperty('maxLength')
      expect(rules).toHaveProperty('url')
      expect(rules).toHaveProperty('getMessage')
    })

    it('getMessage 应该支持参数替换', () => {
      const result = rules.getMessage('required')
      expect(result).toContain('必填')
    })

    it('getMessage 应该对未知key返回默认消息', () => {
      const result = rules.getMessage('nonexistent_key')
      expect(result).toBe('输入格式不正确')
    })

    it('所有规则都应该包含trigger属性', () => {
      const ruleKeys = ['username', 'email', 'phone', 'password', 'strongPassword', 'captcha', 'url']
      ruleKeys.forEach(key => {
        const rule = rules[key]()
        expect(rule).toHaveProperty('trigger')
        expect(Array.isArray(rule.trigger)).toBe(true)
      })
    })
  })
})
